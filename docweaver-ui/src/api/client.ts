import type { AppConfig, DocumentGroup, GeneratedDocument, ImageAsset, OutputType, ProcessResponse } from '../types';

const jsonHeaders = { 'Content-Type': 'application/json' };

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, { cache: 'no-store', ...init });
  if (!res.ok) {
    const payload = await res.json().catch(() => ({}));
    throw new Error(payload.error ?? `Request failed: ${res.status}`);
  }
  if (res.status === 204) {
    return undefined as T;
  }

  const contentType = res.headers.get('content-type') ?? '';
  if (!contentType.toLowerCase().includes('application/json')) {
    return undefined as T;
  }

  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export const api = {
  listImages: () => request<ImageAsset[]>('/api/images'),
  listGroups: () => request<DocumentGroup[]>('/api/groups'),
  listHistory: () => request<GeneratedDocument[]>('/api/history'),
  readConfig: () => request<AppConfig>('/api/config'),
  updateConfig: (config: AppConfig) => request<AppConfig>('/api/config', {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify(config)
  }),
  uploadImages: async (files: File[]) => {
    const form = new FormData();
    files.forEach((f) => form.append('files', f));
    const res = await fetch('/api/images/upload', {
      method: 'POST',
      body: form
    });
    if (!res.ok) {
      const payload = await res.json().catch(() => ({}));
      throw new Error(payload.error ?? 'Upload failed');
    }
    return (await res.json()) as { images: ImageAsset[] };
  },
  renameImage: (imageId: string, displayName: string) => request<ImageAsset>(`/api/images/${imageId}/rename`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify({ displayName })
  }),
  deleteImage: (imageId: string) => request<void>(`/api/images/${imageId}`, {
    method: 'DELETE'
  }),
  updateImageMode: (imageId: string, mode: 'STANDALONE' | 'GROUPED') => request<ImageAsset>(`/api/images/${imageId}/mode`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify({ mode })
  }),
  updateImageRotation: (imageId: string, rotationDegrees: number) => request<ImageAsset>(`/api/images/${imageId}/rotation`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify({ rotationDegrees })
  }),
  createGroup: (name: string, imageIds: string[]) => request<DocumentGroup>('/api/groups', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ name, imageIds })
  }),
  updateGroup: (groupId: string, name: string, imageIds: string[]) => request<DocumentGroup>(`/api/groups/${groupId}`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ name, imageIds })
  }),
  deleteGroup: (groupId: string) => request<void>(`/api/groups/${groupId}`, {
    method: 'DELETE'
  }),
  reorderGroup: (groupId: string, orderedImageIds: string[]) => request<DocumentGroup>(`/api/groups/${groupId}/reorder`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ orderedImageIds })
  }),
  updateGroupImageRotation: (groupId: string, imageId: string, rotationDegrees: number) =>
    request<DocumentGroup>(`/api/groups/${groupId}/images/${imageId}/rotation`, {
      method: 'PATCH',
      headers: jsonHeaders,
      body: JSON.stringify({ rotationDegrees })
    }),
  process: (
    standaloneItems: { imageId: string; outputType: OutputType }[],
    groupIds: string[],
    deleteOriginals: boolean
  ) => request<ProcessResponse>('/api/process', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ standaloneItems, groupIds, deleteOriginals })
  }),
  suggestName: (fileName: string, mimeType: string, fileBytes: string, category: string, grouped: boolean) =>
    request<{ suggestedName: string; confidence: number; source: string }>('/api/suggestions/name', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify({ fileName, mimeType, fileBytes, context: { category, grouped } })
    })
};
