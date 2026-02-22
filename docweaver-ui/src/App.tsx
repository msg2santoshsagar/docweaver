import { useEffect, useMemo, useRef, useState } from 'react';
import { api } from './api/client';
import type { AppConfig, DocumentGroup, GeneratedDocument, ImageAsset, OutputType } from './types';
import { DropColumn } from './components/DropColumn';
import { IconButton, MinusIcon, PlusIcon, ResetViewIcon, RotateLeftIcon, RotateRightIcon, TrashIcon } from './components/IconButton';
import { ImageViewerModal } from './components/ImageViewerModal';
import { Modal } from './components/Modal';

type Tab = 'workspace' | 'history' | 'settings';

type PdfSource =
  | { kind: 'draft' }
  | { kind: 'saved'; groupId: string };
type ProcessStatus = 'idle' | 'running' | 'success' | 'failed';

const UPLOAD_CHUNK_SIZE = 5;

const normalizeRotation = (rotationDegrees: number) => {
  const normalized = rotationDegrees % 360;
  return normalized < 0 ? normalized + 360 : normalized;
};

const sanitizeName = (value: string) =>
  value
    .toLowerCase()
    .replace(/[^a-z0-9._\-\s]/g, ' ')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^[\-._]+|[\-._]+$/g, '') || 'document';

const chunk = <T,>(items: T[], size: number): T[][] => {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    out.push(items.slice(i, i + size));
  }
  return out;
};

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const fileToBase64 = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result?.toString() ?? '';
      resolve(result.includes(',') ? result.split(',')[1] : result);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

export default function App() {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [tab, setTab] = useState<Tab>('workspace');
  const [images, setImages] = useState<ImageAsset[]>([]);
  const [groups, setGroups] = useState<DocumentGroup[]>([]);
  const [history, setHistory] = useState<GeneratedDocument[]>([]);
  const [config, setConfig] = useState<AppConfig | null>(null);

  const [selectedStandalone, setSelectedStandalone] = useState<Record<string, boolean>>({});
  const [deleteOriginals, setDeleteOriginals] = useState(false);

  const [groupEditorId, setGroupEditorId] = useState<string>('');
  const [groupName, setGroupName] = useState('');
  const [groupOrder, setGroupOrder] = useState<string[]>([]);

  const [imageModalId, setImageModalId] = useState<string>('');

  const [pdfModalSource, setPdfModalSource] = useState<PdfSource | null>(null);
  const [pdfSelectedIdx, setPdfSelectedIdx] = useState(0);
  const [pdfZoom, setPdfZoom] = useState(1);
  const [pdfPan, setPdfPan] = useState({ x: 0, y: 0 });
  const [pdfPanning, setPdfPanning] = useState(false);
  const [draftPageRotations, setDraftPageRotations] = useState<Record<string, number>>({});
  const [pdfNameDraft, setPdfNameDraft] = useState('');
  const pdfDragRef = useRef({ active: false, startX: 0, startY: 0, originX: 0, originY: 0 });

  const [confirmRemoveAllPool, setConfirmRemoveAllPool] = useState(false);
  const [confirmDeleteGroup, setConfirmDeleteGroup] = useState<{
    groupId: string;
    groupName: string;
    reason: 'saved-list' | 'last-page';
  } | null>(null);
  const [processStatus, setProcessStatus] = useState<ProcessStatus>('idle');

  const [saving, setSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [bootstrapped, setBootstrapped] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!message) return;
    const id = window.setTimeout(() => setMessage(''), 4000);
    return () => window.clearTimeout(id);
  }, [message]);

  useEffect(() => {
    if (!error) return;
    const id = window.setTimeout(() => setError(''), 5000);
    return () => window.clearTimeout(id);
  }, [error]);

  useEffect(() => {
    if (processStatus === 'idle' || processStatus === 'running') return;
    const id = window.setTimeout(() => setProcessStatus('idle'), 2500);
    return () => window.clearTimeout(id);
  }, [processStatus]);

  useEffect(() => {
    const modalOpen = Boolean(imageModalId) || Boolean(pdfModalSource) || confirmRemoveAllPool || Boolean(confirmDeleteGroup);
    if (!modalOpen) return;

    const previousOverflow = document.body.style.overflow;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setImageModalId('');
        setPdfModalSource(null);
        setConfirmRemoveAllPool(false);
        setConfirmDeleteGroup(null);
      }
    };

    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [imageModalId, pdfModalSource, confirmRemoveAllPool, confirmDeleteGroup]);

  useEffect(() => {
    if (!pdfModalSource) return;
    const onWindowMouseUp = () => {
      pdfDragRef.current.active = false;
      setPdfPanning(false);
    };
    window.addEventListener('mouseup', onWindowMouseUp);
    return () => window.removeEventListener('mouseup', onWindowMouseUp);
  }, [pdfModalSource]);

  const imageById = useMemo(() => new Map(images.map((img) => [img.id, img])), [images]);

  const openImageViewer = (imageId: string) => {
    setImageModalId(imageId);
  };

  const groupedIdsEffective = useMemo(() => {
    const set = new Set<string>();
    for (const group of groups) {
      if (groupEditorId && group.id === groupEditorId) {
        for (const id of groupOrder) set.add(id);
      } else {
        for (const img of group.images) set.add(img.id);
      }
    }
    return set;
  }, [groups, groupEditorId, groupOrder]);

  const groupedInOtherIds = useMemo(() => {
    const set = new Set<string>();
    for (const group of groups) {
      if (groupEditorId && group.id === groupEditorId) continue;
      for (const img of group.images) set.add(img.id);
    }
    return set;
  }, [groups, groupEditorId]);

  const standaloneEligibleIds = useMemo(
    () => images.filter((img) => !groupedIdsEffective.has(img.id)).map((img) => img.id),
    [images, groupedIdsEffective]
  );

  const groupCandidateIds = useMemo(
    () => images.filter((img) => !groupedInOtherIds.has(img.id) || groupOrder.includes(img.id)).map((img) => img.id),
    [images, groupedInOtherIds, groupOrder]
  );

  useEffect(() => {
    const allowedStandalone = new Set(standaloneEligibleIds);
    setSelectedStandalone((prev) => {
      const next: Record<string, boolean> = {};
      for (const [id, asPdf] of Object.entries(prev)) {
        if (allowedStandalone.has(id)) next[id] = asPdf;
      }
      return next;
    });
  }, [standaloneEligibleIds]);

  useEffect(() => {
    const allowedGroup = new Set(groupCandidateIds);
    setGroupOrder((prev) => prev.filter((id) => allowedGroup.has(id)));
  }, [groupCandidateIds]);

  useEffect(() => {
    setDraftPageRotations((prev) => {
      const next: Record<string, number> = {};
      for (const imageId of groupOrder) {
        next[imageId] = normalizeRotation(prev[imageId] ?? 0);
      }
      return next;
    });
  }, [groupOrder]);

  const loadAll = async () => {
    setError('');
    try {
      let configRow: AppConfig | null = null;
      let configError: Error | null = null;
      for (let attempt = 0; attempt < 8; attempt += 1) {
        try {
          configRow = await api.readConfig();
          configError = null;
          break;
        } catch (e) {
          configError = e as Error;
          if (attempt < 7) {
            await wait(500);
          }
        }
      }
      if (!configRow) {
        throw (configError ?? new Error('Failed to load configuration.'));
      }
      setConfig(configRow);
      setDeleteOriginals(configRow.defaultDeleteOriginals);

      const [imageRows, groupRows, historyRows] = await Promise.allSettled([
        api.listImages(),
        api.listGroups(),
        api.listHistory()
      ]);

      if (imageRows.status === 'fulfilled') {
        setImages(imageRows.value);
      } else {
        setError((imageRows.reason as Error)?.message ?? 'Failed to load images.');
      }

      if (groupRows.status === 'fulfilled') {
        setGroups(groupRows.value);
      } else {
        setError((groupRows.reason as Error)?.message ?? 'Failed to load groups.');
      }

      if (historyRows.status === 'fulfilled') {
        setHistory(historyRows.value);
      } else {
        setError((historyRows.reason as Error)?.message ?? 'Failed to load history.');
      }
    } catch (e) {
      setConfig(null);
      setError((e as Error).message);
    }
  };

  const initializeApp = async () => {
    setBootstrapped(false);
    await loadAll();
    setBootstrapped(true);
  };

  useEffect(() => {
    void initializeApp();
  }, []);

  const resetGroupEditor = () => {
    setGroupEditorId('');
    setGroupName('');
    setGroupOrder([]);
    setDraftPageRotations({});
  };

  const handleUpload = async (files: FileList | null) => {
    if (!files || files.length === 0 || !config) return;

    setSaving(true);
    setIsUploading(true);
    setError('');
    setMessage('');

    try {
      const fileArray = Array.from(files);
      const pairs: Array<{ file: File; imageId: string; fallbackName: string }> = [];

      for (const batch of chunk(fileArray, UPLOAD_CHUNK_SIZE)) {
        const uploaded = await api.uploadImages(batch);
        for (let i = 0; i < uploaded.images.length; i += 1) {
          const image = uploaded.images[i];
          const file = batch[i];
          if (image && file) {
            pairs.push({
              file,
              imageId: image.id,
              fallbackName: sanitizeName(file.name.replace(/\.[^.]+$/, ''))
            });
          }
        }
      }

      let nameFailures = 0;
      for (const pair of pairs) {
        let targetName = pair.fallbackName;
        try {
          const bytes = await fileToBase64(pair.file);
          const suggestion = await api.suggestName(
            pair.file.name,
            pair.file.type || 'application/octet-stream',
            bytes,
            'General',
            false
          );
          targetName = suggestion.suggestedName || targetName;
        } catch {
          nameFailures += 1;
        }

        try {
          await api.renameImage(pair.imageId, targetName);
        } catch {
          nameFailures += 1;
        }
      }

      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadAll();
      setMessage(
        nameFailures > 0
          ? `Uploaded ${pairs.length} image(s). Some name suggestions failed.`
          : `Uploaded ${pairs.length} image(s).`
      );
    } catch (e) {
      setError((e as Error).message || 'Upload failed');
    } finally {
      setIsUploading(false);
      setSaving(false);
    }
  };

  const handleRename = async (imageId: string, displayName: string) => {
    try {
      await api.renameImage(imageId, displayName);
      setImages((prev) => prev.map((img) => (img.id === imageId ? { ...img, displayName } : img)));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const handleImageRotation = async (imageId: string, rotationDegrees: number) => {
    try {
      const updated = await api.updateImageRotation(imageId, rotationDegrees);
      setImages((prev) => prev.map((img) => (img.id === updated.id ? updated : img)));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const removeImage = async (imageId: string) => {
    try {
      await api.deleteImage(imageId);
      if (imageModalId === imageId) setImageModalId('');
      setMessage('Image removed from workspace.');
      await loadAll();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const removeAllPoolImages = async () => {
    setConfirmRemoveAllPool(false);
    if (poolIds.length === 0) return;

    setSaving(true);
    try {
      const results = await Promise.allSettled(poolIds.map((id) => api.deleteImage(id)));
      const failed = results.filter((r) => r.status === 'rejected').length;
      await loadAll();
      setMessage(
        failed === 0
          ? `Removed ${poolIds.length} image(s) from pool.`
          : `Removed ${poolIds.length - failed} image(s), ${failed} failed.`
      );
    } finally {
      setSaving(false);
    }
  };

  const addToStandalone = (imageId: string) => {
    if (!standaloneEligibleIds.includes(imageId)) return;
    setGroupOrder((prev) => prev.filter((id) => id !== imageId));
    setSelectedStandalone((prev) => {
      const defaultAsPdf = config?.defaultStandaloneOutputType === 'PDF';
      return { ...prev, [imageId]: prev[imageId] ?? defaultAsPdf };
    });
  };

  const toggleStandalonePdf = (imageId: string) => {
    setSelectedStandalone((prev) => ({ ...prev, [imageId]: !prev[imageId] }));
  };

  const addToGroupDraft = (imageId: string) => {
    if (!groupCandidateIds.includes(imageId)) return;
    setSelectedStandalone((prev) => {
      const next = { ...prev };
      delete next[imageId];
      return next;
    });
    setGroupOrder((prev) => (prev.includes(imageId) ? prev : [...prev, imageId]));
  };

  const returnToPool = (imageId: string) => {
    setSelectedStandalone((prev) => {
      const next = { ...prev };
      delete next[imageId];
      return next;
    });
    setGroupOrder((prev) => prev.filter((id) => id !== imageId));
  };

  const saveGroup = async () => {
    setSaving(true);
    setError('');
    try {
      if (!groupName.trim() || groupOrder.length === 0) {
        throw new Error('Group name and at least one image are required');
      }

      let savedGroup: DocumentGroup;
      if (groupEditorId) {
        savedGroup = await api.updateGroup(groupEditorId, groupName.trim(), groupOrder);
      } else {
        savedGroup = await api.createGroup(groupName.trim(), groupOrder);
      }

      const rotationUpdates = groupOrder.map(async (imageId) => {
        const rotation = normalizeRotation(draftPageRotations[imageId] ?? 0);
        if (rotation === 0) return;
        await api.updateGroupImageRotation(savedGroup.id, imageId, rotation);
      });
      await Promise.all(rotationUpdates);

      if (!groupEditorId) {
        setPdfModalSource({ kind: 'saved', groupId: savedGroup.id });
        setPdfSelectedIdx(0);
      }

      setMessage('Group saved');
      resetGroupEditor();
      await loadAll();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const loadGroupInEditor = (group: DocumentGroup) => {
    setGroupEditorId(group.id);
    setGroupName(group.name);
    setGroupOrder(group.images.map((img) => img.id));
    setDraftPageRotations(group.rotationByImageId ?? {});
    setPdfModalSource({ kind: 'saved', groupId: group.id });
    setPdfSelectedIdx(0);
  };

  const reorderDraft = (startIdx: number, endIdx: number) => {
    if (startIdx === endIdx || endIdx < 0 || startIdx < 0) return;
    setGroupOrder((prev) => {
      const next = [...prev];
      const [moved] = next.splice(startIdx, 1);
      next.splice(endIdx, 0, moved);
      return next;
    });
  };

  const reorderSavedGroup = async (groupId: string, startIdx: number, endIdx: number) => {
    const group = groups.find((g) => g.id === groupId);
    if (!group || startIdx === endIdx || endIdx < 0) return;

    const next = [...group.images];
    const [moved] = next.splice(startIdx, 1);
    next.splice(endIdx, 0, moved);

    try {
      await api.reorderGroup(groupId, next.map((img) => img.id));
      await loadAll();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const persistGroupRotation = async (targetGroupId: string, imageId: string, rotationDegrees: number) => {
    try {
      const updated = await api.updateGroupImageRotation(targetGroupId, imageId, rotationDegrees);
      setGroups((prev) => prev.map((g) => (g.id === updated.id ? updated : g)));
      if (groupEditorId === updated.id) {
        const syncedRotation = normalizeRotation(updated.rotationByImageId?.[imageId] ?? rotationDegrees);
        setDraftPageRotations((prev) => ({ ...prev, [imageId]: syncedRotation }));
      }
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const rotatePdfPage = async (deltaDegrees: number) => {
    const page = selectedPdfImage;
    if (!pdfModalSource || !page) return;

    if (pdfModalSource.kind === 'draft') {
      const next = normalizeRotation((draftPageRotations[page.id] ?? 0) + deltaDegrees);
      setDraftPageRotations((prev) => ({ ...prev, [page.id]: next }));
      if (groupEditorId) {
        await persistGroupRotation(groupEditorId, page.id, next);
      }
      return;
    }

    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    if (!group) return;
    const current = group.rotationByImageId?.[page.id] ?? 0;
    const next = normalizeRotation(current + deltaDegrees);
    await persistGroupRotation(group.id, page.id, next);
  };

  const removePdfPage = async () => {
    const page = selectedPdfImage;
    if (!pdfModalSource || !page) return;

    if (pdfModalSource.kind === 'draft') {
      setGroupOrder((prev) => prev.filter((id) => id !== page.id));
      setDraftPageRotations((prev) => {
        const next = { ...prev };
        delete next[page.id];
        return next;
      });
      setPdfSelectedIdx((idx) => Math.max(0, idx - 1));
      return;
    }

    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    if (!group) return;
    const nextIds = group.images.map((img) => img.id).filter((id) => id !== page.id);
    if (nextIds.length === 0) {
      setConfirmDeleteGroup({
        groupId: group.id,
        groupName: group.name,
        reason: 'last-page'
      });
      return;
    }

    try {
      const updated = await api.updateGroup(group.id, group.name, nextIds);
      setGroups((prev) => prev.map((g) => (g.id === updated.id ? updated : g)));
      setPdfSelectedIdx((idx) => Math.max(0, Math.min(idx, updated.images.length - 1)));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const requestDeleteGroup = (groupId: string, reason: 'saved-list' | 'last-page' = 'saved-list') => {
    const group = groups.find((g) => g.id === groupId);
    if (!group) return;
    setConfirmDeleteGroup({
      groupId: group.id,
      groupName: group.name,
      reason
    });
  };

  const deleteGroupConfirmed = async () => {
    if (!confirmDeleteGroup) return;

    const { groupId, groupName, reason } = confirmDeleteGroup;
    const groupToDelete = groups.find((g) => g.id === groupId);
    if (!groupToDelete) {
      setConfirmDeleteGroup(null);
      return;
    }
    const deletedImageIds = new Set(groupToDelete.images.map((img) => img.id));
    const previousGroups = groups;
    const previousImages = images;
    setSaving(true);
    setConfirmDeleteGroup(null);
    setError('');
    setGroups((prev) => prev.filter((g) => g.id !== groupId));
    setImages((prev) => prev.map((img) => (deletedImageIds.has(img.id) ? { ...img, mode: 'STANDALONE' } : img)));
    if (groupEditorId === groupId) {
      resetGroupEditor();
    }
    if ((pdfModalSource?.kind === 'saved' && pdfModalSource.groupId === groupId) || reason === 'last-page') {
      setPdfModalSource(null);
      setPdfPan({ x: 0, y: 0 });
      setPdfPanning(false);
    }
    try {
      await api.deleteGroup(groupId);
      setMessage(`Group "${groupName}" deleted. Images returned to pool.`);
    } catch (e) {
      setGroups(previousGroups);
      setImages(previousImages);
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const runProcessing = async () => {
    setSaving(true);
    setProcessStatus('running');
    setError('');
    setMessage('');
    try {
      const allowedStandalone = new Set(standaloneEligibleIds);
      const standaloneItems = Object.entries(selectedStandalone)
        .filter(([imageId]) => allowedStandalone.has(imageId))
        .map(([imageId, asPdf]) => ({ imageId, outputType: (asPdf ? 'PDF' : 'IMAGE') as OutputType }));

      const groupIds = groups.map((group) => group.id);
      const res = await api.process(standaloneItems, groupIds, deleteOriginals);

      setMessage(
        res.success
          ? `Processing complete (${res.results.length} outputs)`
          : `Processing completed with failures (${res.results.filter((r) => r.status === 'FAILED').length} failed)`
      );
      setProcessStatus(res.success ? 'success' : 'failed');
      await loadAll();
    } catch (e) {
      setProcessStatus('failed');
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const saveConfig = async () => {
    if (!config) return;
    setSaving(true);
    setError('');
    try {
      const saved = await api.updateConfig(config);
      setConfig(saved);
      setMessage('Settings saved');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const standaloneIds = useMemo(() => Object.keys(selectedStandalone), [selectedStandalone]);

  const poolIds = useMemo(() => {
    const standaloneSet = new Set(standaloneIds);
    const groupSet = new Set(groupOrder);
    return images
      .map((img) => img.id)
      .filter((id) => groupCandidateIds.includes(id) && !standaloneSet.has(id) && !groupSet.has(id));
  }, [images, groupCandidateIds, standaloneIds, groupOrder]);

  const processPreview = useMemo(() => {
    if (!config) return [];

    const rows: Array<{ key: string; name: string; format: string; path: string; deleteOriginals: boolean }> = [];

    for (const [imageId, asPdf] of Object.entries(selectedStandalone)) {
      const image = imageById.get(imageId);
      if (!image || !standaloneEligibleIds.includes(imageId)) continue;

      const outputType = asPdf ? 'PDF' : 'IMAGE';
      const ext = outputType === 'PDF' ? 'pdf' : (image.originalFileName.split('.').pop() || 'img').toLowerCase();
      const name = `${sanitizeName(image.displayName)}.${ext}`;
      rows.push({ key: imageId, name, format: outputType, path: `${config.outputFolder}/${name}`, deleteOriginals });
    }

    for (const group of groups) {
      const name = `${sanitizeName(group.name)}.pdf`;
      rows.push({ key: group.id, name, format: 'PDF', path: `${config.outputFolder}/${name}`, deleteOriginals });
    }

    return rows;
  }, [config, deleteOriginals, selectedStandalone, imageById, standaloneEligibleIds, groups]);

  const imageModalImage = imageModalId ? imageById.get(imageModalId) : undefined;

  const pdfModalImages = useMemo(() => {
    if (!pdfModalSource) return [];
    if (pdfModalSource.kind === 'draft') {
      return groupOrder.map((id) => imageById.get(id)).filter(Boolean) as ImageAsset[];
    }
    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    return group ? group.images : [];
  }, [pdfModalSource, groupOrder, imageById, groups]);

  useEffect(() => {
    setPdfSelectedIdx((idx) => Math.max(0, Math.min(idx, Math.max(0, pdfModalImages.length - 1))));
  }, [pdfModalImages.length]);

  const selectedPdfImage = pdfModalImages[pdfSelectedIdx];
  const selectedPdfRotation = useMemo(() => {
    if (!selectedPdfImage || !pdfModalSource) return 0;
    if (pdfModalSource.kind === 'draft') {
      return normalizeRotation(draftPageRotations[selectedPdfImage.id] ?? 0);
    }
    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    return normalizeRotation(group?.rotationByImageId?.[selectedPdfImage.id] ?? 0);
  }, [selectedPdfImage, pdfModalSource, draftPageRotations, groups]);

  useEffect(() => {
    if (!pdfModalSource) {
      setPdfNameDraft('');
      return;
    }
    if (pdfModalSource.kind === 'draft') {
      setPdfNameDraft(groupName);
      return;
    }
    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    setPdfNameDraft(group?.name ?? '');
  }, [pdfModalSource, groups, groupName]);

  useEffect(() => {
    setPdfPan({ x: 0, y: 0 });
    setPdfPanning(false);
    pdfDragRef.current.active = false;
  }, [selectedPdfImage?.id, pdfModalSource?.kind]);

  const commitPdfName = async () => {
    const trimmed = pdfNameDraft.trim();
    if (!pdfModalSource) return;

    if (pdfModalSource.kind === 'draft') {
      if (!trimmed) {
        setPdfNameDraft(groupName);
        return;
      }
      setGroupName(trimmed);
      setPdfNameDraft(trimmed);
      return;
    }

    const group = groups.find((g) => g.id === pdfModalSource.groupId);
    if (!group) return;

    if (!trimmed) {
      setPdfNameDraft(group.name);
      return;
    }
    if (trimmed !== group.name) {
      try {
        const updated = await api.updateGroup(group.id, trimmed, group.images.map((img) => img.id));
        setGroups((prev) => prev.map((g) => (g.id === updated.id ? updated : g)));
        if (groupEditorId === updated.id) {
          setGroupName(updated.name);
        }
        setPdfNameDraft(updated.name);
      } catch (e) {
        setError((e as Error).message);
      }
    }
  };

  if (!bootstrapped && !config) return <div className="p-10 text-text">Loading...</div>;

  if (bootstrapped && !config) {
    return (
      <div className="mx-auto mt-20 max-w-lg rounded-2xl border border-danger/40 bg-panel p-8 text-text shadow-card">
        <h2 className="text-xl font-semibold">Unable to load DocWeaver</h2>
        <p className="mt-2 text-sm text-muted">
          Could not load application configuration from the backend. Check Docker logs, then retry.
        </p>
        {error && <p className="mt-3 rounded bg-danger/10 px-3 py-2 text-sm text-danger">{error}</p>}
        <button
          className="mt-5 rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-bg"
          onClick={() => void initializeApp()}
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 pb-12 pt-8 text-text sm:px-8">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">DocWeaver</h1>
          <p className="text-sm text-muted">Smart local image routing and reliable document generation.</p>
        </div>
        <nav className="flex gap-2 rounded-xl bg-panel p-1 shadow-card">
          {(['workspace', 'history', 'settings'] as Tab[]).map((t) => (
            <button
              key={t}
              className={`rounded-lg px-4 py-2 text-sm capitalize transition ${tab === t ? 'bg-accent text-bg' : 'text-muted hover:text-text'}`}
              onClick={() => setTab(t)}
            >
              {t}
            </button>
          ))}
        </nav>
      </header>

      {error && (
        <div className="mb-4 flex items-center justify-between rounded-lg border border-danger/40 bg-danger/10 p-3 text-sm text-danger">
          <span>{error}</span>
          <button className="ml-3 rounded px-2 py-1 text-danger/90 hover:bg-danger/20" onClick={() => setError('')}>✕</button>
        </div>
      )}
      {message && (
        <div className="mb-4 flex items-center justify-between rounded-lg border border-success/40 bg-success/10 p-3 text-sm text-success">
          <span>{message}</span>
          <button className="ml-3 rounded px-2 py-1 text-success/90 hover:bg-success/20" onClick={() => setMessage('')}>✕</button>
        </div>
      )}

      {tab === 'workspace' && (
        <section className="grid items-start gap-6 lg:grid-cols-[1.35fr_1fr]">
          <div className="space-y-6">
            <div className="rounded-2xl bg-panel p-5 shadow-card">
              <h2 className="mb-3 text-lg font-medium">Upload Images</h2>
              <div className="flex items-center justify-between gap-3">
                <div className="text-sm text-muted">Choose one or more images.</div>
                <div className="flex items-center gap-2">
                  <input
                    ref={fileInputRef}
                    className="hidden"
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={(e) => void handleUpload(e.target.files)}
                    disabled={saving || processStatus === 'running'}
                  />
                  <button
                    className="rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-bg shadow-card disabled:opacity-60"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={saving || processStatus === 'running'}
                  >
                    {isUploading ? 'Uploading...' : 'Select Images'}
                  </button>
                </div>
              </div>
            </div>

            <div className="rounded-2xl bg-panel p-5 shadow-card">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h2 className="text-lg font-medium">Image Routing</h2>
                <div className="flex items-center gap-2 text-xs text-muted">
                  <span>Pool: {poolIds.length}</span>
                  <span>Standalone: {standaloneIds.length}</span>
                  <span>Draft: {groupOrder.length}</span>
                  <button
                    className="inline-flex items-center gap-1 rounded-md border border-danger/35 bg-transparent px-2 py-1 font-medium text-danger hover:bg-danger/10 disabled:opacity-50"
                    onClick={() => setConfirmRemoveAllPool(true)}
                    disabled={poolIds.length === 0 || saving}
                  >
                    <TrashIcon />
                    Remove All Pool
                  </button>
                </div>
              </div>

              <div className="mb-3 grid gap-2 sm:grid-cols-3">
                <input
                  className="rounded-lg border border-panelSoft bg-panelSoft px-3 py-2 text-sm"
                  placeholder="Group name"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                />
                <button className="rounded-lg bg-panelSoft px-3 py-2 text-sm" onClick={resetGroupEditor}>Reset Draft</button>
                <button className="rounded-lg bg-accent px-3 py-2 text-sm font-medium text-bg" onClick={() => void saveGroup()}>
                  {groupEditorId ? 'Update Group' : 'Create Group'}
                </button>
              </div>

              <div className="grid gap-3 lg:grid-cols-3">
                <DropColumn
                  title="Image Pool"
                  subtitle="Unassigned"
                  ids={poolIds}
                  imageById={imageById}
                  onDropImage={returnToPool}
                  onCardClick={openImageViewer}
                  renderActions={(id) => (
                    <IconButton
                      title="Delete image"
                      danger
                      onClick={(e) => {
                        e.stopPropagation();
                        void removeImage(id);
                      }}
                    >
                      <TrashIcon />
                    </IconButton>
                  )}
                />

                <DropColumn
                  title="Standalone"
                  subtitle="Single output"
                  ids={standaloneIds}
                  imageById={imageById}
                  onDropImage={addToStandalone}
                  onCardClick={openImageViewer}
                  renderExtra={(id) => (
                    <label className="mt-1 flex items-center gap-2 text-[11px] text-muted">
                      <input type="checkbox" checked={Boolean(selectedStandalone[id])} onChange={() => toggleStandalonePdf(id)} />
                      Create PDF
                    </label>
                  )}
                  renderActions={(id) => (
                    <button
                      className="rounded bg-panelSoft px-2 py-1 text-[11px]"
                      onClick={(e) => {
                        e.stopPropagation();
                        returnToPool(id);
                      }}
                    >
                      Move
                    </button>
                  )}
                />

                <DropColumn
                  title={groupEditorId ? 'Group Draft (Editing)' : 'Group Draft'}
                  subtitle="Grouped PDF"
                  ids={groupOrder}
                  imageById={imageById}
                  onDropImage={addToGroupDraft}
                  onCardClick={openImageViewer}
                  onReorder={reorderDraft}
                  renderActions={(id) => (
                    <button
                      className="rounded bg-panelSoft px-2 py-1 text-[11px]"
                      onClick={(e) => {
                        e.stopPropagation();
                        returnToPool(id);
                      }}
                    >
                      Move
                    </button>
                  )}
                />
              </div>

              <div className="mt-3">
                <button
                  className="rounded bg-panelSoft px-3 py-1 text-xs"
                  onClick={() => {
                    setPdfModalSource({ kind: 'draft' });
                    setPdfSelectedIdx(0);
                    setPdfZoom(1);
                    setPdfPan({ x: 0, y: 0 });
                  }}
                  disabled={groupOrder.length === 0}
                >
                  Open Draft PDF Preview
                </button>
              </div>
            </div>

            <div className="rounded-2xl bg-panel p-5 shadow-card">
              <h2 className="mb-3 text-lg font-medium">Saved Groups</h2>
              <div className="space-y-3">
                {groups.map((group) => (
                  <div key={group.id} className="rounded-lg border border-panelSoft bg-panelSoft/40 p-3">
                    <div className="mb-2 flex items-center justify-between gap-3">
                      <button
                        className="text-left"
                        onClick={() => {
                          setPdfModalSource({ kind: 'saved', groupId: group.id });
                          setPdfSelectedIdx(0);
                          setPdfZoom(1);
                          setPdfPan({ x: 0, y: 0 });
                        }}
                      >
                        <div className="font-medium">{group.name}</div>
                        <div className="text-xs text-muted">{group.images.length} page(s)</div>
                      </button>
                      <div className="flex items-center gap-3">
                        <span className="rounded-full border border-success/40 bg-success/10 px-2 py-0.5 text-[11px] text-success">
                          Queued
                        </span>
                        <button className="text-xs text-accent" onClick={() => loadGroupInEditor(group)}>Edit</button>
                        <IconButton
                          title="Delete group"
                          danger
                          onClick={(e) => {
                            e.stopPropagation();
                            requestDeleteGroup(group.id, 'saved-list');
                          }}
                        >
                          <TrashIcon />
                        </IconButton>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <aside className="sticky top-4 space-y-6">
            <div className="rounded-2xl bg-panel p-5 shadow-card">
              <h2 className="mb-3 text-lg font-medium">Process Queue</h2>
              <label className="mb-3 flex items-center gap-2 text-sm text-muted">
                <input type="checkbox" checked={deleteOriginals} onChange={(e) => setDeleteOriginals(e.target.checked)} />
                Delete originals after full success
              </label>
              <div className="max-h-[22rem] space-y-2 overflow-auto">
                {processPreview.length === 0 && <div className="text-sm text-muted">No items to process.</div>}
                {processPreview.map((row) => (
                  <div key={row.key} className="rounded-lg border border-panelSoft bg-panelSoft/50 p-2 text-xs">
                    <div className="font-medium">{row.name}</div>
                    <div className="text-muted">{row.path}</div>
                    <div className="text-muted">{row.format} • delete originals: {row.deleteOriginals ? 'YES' : 'NO'}</div>
                  </div>
                ))}
              </div>
              <button
                type="button"
                className="mt-4 flex w-full cursor-pointer items-center justify-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-bg transition disabled:cursor-not-allowed disabled:opacity-60"
                disabled={saving || processStatus === 'running' || processPreview.length === 0}
                onClick={() => void runProcessing()}
              >
                {processStatus === 'running' && (
                  <span aria-hidden className="pointer-events-none inline-block h-4 w-4 animate-spin rounded-full border-2 border-bg/30 border-t-bg" />
                )}
                {processStatus === 'success' && <span aria-hidden>✓</span>}
                {processStatus === 'running'
                  ? 'Processing...'
                  : processStatus === 'success'
                    ? 'Processed'
                    : processStatus === 'failed'
                      ? 'Retry Process'
                      : 'Process'}
              </button>
            </div>
          </aside>
        </section>
      )}

      {tab === 'history' && (
        <section className="rounded-2xl bg-panel p-5 shadow-card">
          <h2 className="mb-3 text-lg font-medium">Processing History</h2>
          <div className="overflow-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="text-muted">
                  <th className="pb-2">Status</th>
                  <th className="pb-2">Type</th>
                  <th className="pb-2">Output</th>
                  <th className="pb-2">Flags</th>
                  <th className="pb-2">Time</th>
                </tr>
              </thead>
              <tbody>
                {history.map((row) => (
                  <tr key={row.id} className="border-t border-panelSoft">
                    <td className={`py-2 ${row.status === 'SUCCESS' ? 'text-success' : 'text-danger'}`}>{row.status}</td>
                    <td className="py-2">{row.type}</td>
                    <td className="py-2">
                      <div>{row.outputName}</div>
                      <div className="text-xs text-muted">{row.outputPath}</div>
                      <div className="text-xs text-muted">{row.message}</div>
                    </td>
                    <td className="py-2 text-xs text-muted">
                      delete: {row.deleteOriginals ? 'yes' : 'no'}<br />dry-run: {row.dryRun ? 'yes' : 'no'}
                    </td>
                    <td className="py-2 text-xs text-muted">{new Date(row.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === 'settings' && config && (
        <section className="rounded-2xl bg-panel p-5 shadow-card">
          <h2 className="mb-4 text-lg font-medium">Settings</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-1 text-sm">
              <span className="text-muted">Default output folder</span>
              <input
                className="w-full rounded-lg border border-panelSoft bg-panelSoft px-3 py-2"
                value={config.outputFolder}
                onChange={(e) => setConfig({ ...config, outputFolder: e.target.value })}
              />
            </label>

            <label className="space-y-1 text-sm">
              <span className="text-muted">Default standalone output type</span>
              <select
                className="w-full rounded-lg border border-panelSoft bg-panelSoft px-3 py-2"
                value={config.defaultStandaloneOutputType}
                onChange={(e) => setConfig({ ...config, defaultStandaloneOutputType: e.target.value as OutputType })}
              >
                <option value="IMAGE">IMAGE</option>
                <option value="PDF">PDF</option>
              </select>
            </label>

            <label className="flex items-center gap-2 text-sm text-muted">
              <input
                type="checkbox"
                checked={config.defaultDeleteOriginals}
                onChange={(e) => setConfig({ ...config, defaultDeleteOriginals: e.target.checked })}
              />
              Default delete originals behavior
            </label>

            <label className="flex items-center gap-2 text-sm text-muted">
              <input
                type="checkbox"
                checked={config.dryRun}
                onChange={(e) => setConfig({ ...config, dryRun: e.target.checked })}
              />
              Dry-run mode (validate only)
            </label>
          </div>

          <button className="mt-5 rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-bg" onClick={() => void saveConfig()}>
            Save Settings
          </button>
        </section>
      )}

      <ImageViewerModal
        isOpen={Boolean(imageModalImage)}
        image={imageModalImage}
        onClose={() => setImageModalId('')}
        onDelete={(id) => void removeImage(id)}
        onRename={(id, displayName) => void handleRename(id, displayName)}
        onRotate={(id, rotationDegrees) => void handleImageRotation(id, rotationDegrees)}
      />

      <Modal
        isOpen={Boolean(pdfModalSource)}
        onClose={() => {
          setPdfModalSource(null);
          setPdfPan({ x: 0, y: 0 });
          setPdfPanning(false);
        }}
        title={pdfModalSource?.kind === 'draft' ? 'Draft PDF Preview' : 'Group PDF Preview'}
      >
        <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
          <div className="max-h-[72vh] space-y-2 overflow-auto rounded-lg border border-panelSoft bg-panel p-2">
            {pdfModalImages.map((img, idx) => (
              <button
                key={img.id}
                draggable
                onDragStart={(e) => e.dataTransfer.setData('text/plain', String(idx))}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  const from = Number(e.dataTransfer.getData('text/plain'));
                  if (Number.isNaN(from)) return;
                  if (pdfModalSource?.kind === 'draft') {
                    reorderDraft(from, idx);
                  } else if (pdfModalSource?.kind === 'saved') {
                    void reorderSavedGroup(pdfModalSource.groupId, from, idx);
                  }
                }}
                className={`w-full rounded p-1 text-left ${idx === pdfSelectedIdx ? 'bg-accent/20' : 'bg-panelSoft/50'}`}
                onClick={() => setPdfSelectedIdx(idx)}
              >
                <div className="mb-1 text-[11px] text-muted">Page {idx + 1}</div>
                <img src={`/api/images/${img.id}/preview`} alt={img.displayName} className="h-20 w-full rounded object-cover" />
                <div className="truncate text-xs">{img.displayName}</div>
              </button>
            ))}
          </div>

          <div className="rounded-lg border border-panelSoft bg-panel p-3">
            <input
              className="mb-2 w-full rounded border border-panelSoft bg-panelSoft px-3 py-2 text-sm font-medium"
              value={pdfNameDraft}
              placeholder="PDF name"
              onChange={(e) => setPdfNameDraft(e.target.value)}
              onBlur={() => void commitPdfName()}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  (e.target as HTMLInputElement).blur();
                }
              }}
            />
            <div className="mb-2 flex items-center gap-2">
              <IconButton title="Zoom out" onClick={() => setPdfZoom((z) => Math.max(0.25, z - 0.1))}><MinusIcon /></IconButton>
              <span className="text-xs text-muted">{Math.round(pdfZoom * 100)}%</span>
              <IconButton title="Zoom in" onClick={() => setPdfZoom((z) => Math.min(3, z + 0.1))}><PlusIcon /></IconButton>
              <IconButton title="Rotate left" onClick={() => void rotatePdfPage(-90)}><RotateLeftIcon /></IconButton>
              <IconButton title="Rotate right" onClick={() => void rotatePdfPage(90)}><RotateRightIcon /></IconButton>
              <div className="ml-auto" />
              <IconButton
                title="Reset page orientation and zoom"
                onClick={() => {
                  setPdfZoom(1);
                  setPdfPan({ x: 0, y: 0 });
                  void rotatePdfPage(-selectedPdfRotation);
                }}
              >
                <ResetViewIcon />
              </IconButton>
              <IconButton title="Remove page from group" danger onClick={() => void removePdfPage()}><TrashIcon /></IconButton>
            </div>

            <div
              className="flex h-[65vh] items-center justify-center overflow-hidden rounded bg-black/30"
              onWheel={(e) => {
                e.preventDefault();
                setPdfZoom((z) => {
                  const next = z + (e.deltaY < 0 ? 0.08 : -0.08);
                  return Math.max(0.25, Math.min(3, next));
                });
              }}
              onMouseDown={(e) => {
                if (e.button !== 0 || pdfZoom <= 1 || !selectedPdfImage) return;
                e.preventDefault();
                pdfDragRef.current = {
                  active: true,
                  startX: e.clientX,
                  startY: e.clientY,
                  originX: pdfPan.x,
                  originY: pdfPan.y
                };
                setPdfPanning(true);
              }}
              onMouseMove={(e) => {
                if (!pdfDragRef.current.active) return;
                if ((e.buttons & 1) !== 1) {
                  pdfDragRef.current.active = false;
                  setPdfPanning(false);
                  return;
                }
                const dx = e.clientX - pdfDragRef.current.startX;
                const dy = e.clientY - pdfDragRef.current.startY;
                setPdfPan({ x: pdfDragRef.current.originX + dx, y: pdfDragRef.current.originY + dy });
              }}
              onMouseUp={() => {
                pdfDragRef.current.active = false;
                setPdfPanning(false);
              }}
              onMouseLeave={() => {
                pdfDragRef.current.active = false;
                setPdfPanning(false);
              }}
              style={{ cursor: pdfZoom > 1 ? (pdfPanning ? 'grabbing' : 'grab') : 'default' }}
            >
              {selectedPdfImage ? (
                <img
                  src={`/api/images/${selectedPdfImage.id}/preview`}
                  alt={selectedPdfImage.displayName}
                  draggable={false}
                  onDragStart={(e) => e.preventDefault()}
                  style={{
                    transform: `translate(${pdfPan.x}px, ${pdfPan.y}px) scale(${pdfZoom}) rotate(${selectedPdfRotation}deg)`,
                    willChange: 'transform',
                    userSelect: 'none'
                  }}
                  className="max-h-full max-w-full object-contain"
                />
              ) : (
                <div className="text-sm text-muted">No pages</div>
              )}
            </div>
          </div>
        </div>
      </Modal>

      <Modal isOpen={confirmRemoveAllPool} onClose={() => setConfirmRemoveAllPool(false)} title="Remove All Pool Images?" size="sm">
        <p className="text-sm text-muted">This will remove all images currently in the pool from workspace metadata. Continue?</p>
        <div className="mt-4 flex justify-end gap-2">
          <button className="rounded bg-panelSoft px-3 py-2 text-sm" onClick={() => setConfirmRemoveAllPool(false)}>Cancel</button>
          <button className="rounded bg-danger/20 px-3 py-2 text-sm text-danger" onClick={() => void removeAllPoolImages()}>Remove All</button>
        </div>
      </Modal>

      <Modal
        isOpen={Boolean(confirmDeleteGroup)}
        onClose={() => setConfirmDeleteGroup(null)}
        title={confirmDeleteGroup?.reason === 'last-page' ? 'Delete Group?' : 'Remove Saved Group?'}
        size="sm"
      >
        <p className="text-sm text-muted">
          {confirmDeleteGroup?.reason === 'last-page'
            ? `This is the last page in "${confirmDeleteGroup.groupName}". Deleting it will delete the entire group and return the image to the pool. Continue?`
            : `Delete group "${confirmDeleteGroup?.groupName}"? All images in this group will be returned to the pool.`}
        </p>
        <div className="mt-4 flex justify-end gap-2">
          <button className="rounded bg-panelSoft px-3 py-2 text-sm" onClick={() => setConfirmDeleteGroup(null)}>
            Cancel
          </button>
          <button className="rounded bg-danger/20 px-3 py-2 text-sm text-danger" onClick={() => void deleteGroupConfirmed()}>
            Delete Group
          </button>
        </div>
      </Modal>
    </div>
  );
}
