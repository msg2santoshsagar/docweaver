import { useEffect, useRef, useState } from 'react';
import type { ImageAsset } from '../types';
import { IconButton, MinusIcon, PlusIcon, ResetViewIcon, RotateLeftIcon, RotateRightIcon, TrashIcon } from './IconButton';
import { Modal } from './Modal';

type ImageViewerModalProps = {
  image?: ImageAsset;
  isOpen: boolean;
  onClose: () => void;
  onDelete: (id: string) => void | Promise<void>;
  onRename: (id: string, displayName: string) => void | Promise<void>;
};

export function ImageViewerModal({ image, isOpen, onClose, onDelete, onRename }: ImageViewerModalProps) {
  const [zoom, setZoom] = useState(1);
  const [rotate, setRotate] = useState(0);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [panning, setPanning] = useState(false);
  const [nameDraft, setNameDraft] = useState('');
  const dragRef = useRef({ active: false, startX: 0, startY: 0, originX: 0, originY: 0 });

  useEffect(() => {
    if (!image) return;
    setZoom(1);
    setRotate(0);
    setPan({ x: 0, y: 0 });
    setPanning(false);
    setNameDraft(image.displayName);
  }, [image?.id]);

  useEffect(() => {
    const onWindowMouseUp = () => {
      dragRef.current.active = false;
      setPanning(false);
    };
    if (!isOpen) return;
    window.addEventListener('mouseup', onWindowMouseUp);
    return () => window.removeEventListener('mouseup', onWindowMouseUp);
  }, [isOpen]);

  if (!image) return null;

  const commitName = async () => {
    const trimmed = nameDraft.trim();
    if (!trimmed) {
      setNameDraft(image.displayName);
      return;
    }
    if (trimmed !== image.displayName) {
      await onRename(image.id, trimmed);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Image Viewer">
      <div className="space-y-3">
        <input
          className="w-full rounded border border-panelSoft bg-panelSoft px-3 py-2 text-sm font-medium"
          value={nameDraft}
          onChange={(e) => setNameDraft(e.target.value)}
          onBlur={() => void commitName()}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              (e.target as HTMLInputElement).blur();
            }
          }}
        />

        <div className="flex items-center gap-2">
          <IconButton title="Zoom out" onClick={() => setZoom((z) => Math.max(0.25, z - 0.1))}><MinusIcon /></IconButton>
          <span className="text-xs text-muted">{Math.round(zoom * 100)}%</span>
          <IconButton title="Zoom in" onClick={() => setZoom((z) => Math.min(3, z + 0.1))}><PlusIcon /></IconButton>
          <IconButton title="Rotate left" onClick={() => setRotate((r) => r - 90)}><RotateLeftIcon /></IconButton>
          <IconButton title="Rotate right" onClick={() => setRotate((r) => r + 90)}><RotateRightIcon /></IconButton>
          <div className="ml-auto" />
          <IconButton
            title="Reset view and name"
            onClick={() => {
              setZoom(1);
              setRotate(0);
              setPan({ x: 0, y: 0 });
              setPanning(false);
              setNameDraft(image.displayName);
            }}
          >
            <ResetViewIcon />
          </IconButton>
          <IconButton title="Delete image" danger onClick={() => void onDelete(image.id)}><TrashIcon /></IconButton>
        </div>

        <div
          className="flex h-[70vh] items-center justify-center overflow-hidden rounded border border-panelSoft bg-black/30"
          onWheel={(e) => {
            e.preventDefault();
            setZoom((z) => {
              const next = z + (e.deltaY < 0 ? 0.08 : -0.08);
              return Math.max(0.25, Math.min(3, next));
            });
          }}
          onMouseDown={(e) => {
            if (e.button !== 0 || zoom <= 1) return;
            e.preventDefault();
            dragRef.current = {
              active: true,
              startX: e.clientX,
              startY: e.clientY,
              originX: pan.x,
              originY: pan.y
            };
            setPanning(true);
          }}
          onMouseMove={(e) => {
            if (!dragRef.current.active) return;
            if ((e.buttons & 1) !== 1) {
              dragRef.current.active = false;
              setPanning(false);
              return;
            }
            const dx = e.clientX - dragRef.current.startX;
            const dy = e.clientY - dragRef.current.startY;
            setPan({ x: dragRef.current.originX + dx, y: dragRef.current.originY + dy });
          }}
          onMouseUp={() => {
            dragRef.current.active = false;
            setPanning(false);
          }}
          onMouseLeave={() => {
            dragRef.current.active = false;
            setPanning(false);
          }}
          style={{ cursor: zoom > 1 ? (panning ? 'grabbing' : 'grab') : 'default' }}
        >
          <img
            src={`/api/images/${image.id}/preview`}
            alt={image.displayName}
            draggable={false}
            onDragStart={(e) => e.preventDefault()}
            style={{
              transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom}) rotate(${rotate}deg)`,
              willChange: 'transform',
              userSelect: 'none'
            }}
            className="max-h-full max-w-full object-contain"
          />
        </div>
      </div>
    </Modal>
  );
}
