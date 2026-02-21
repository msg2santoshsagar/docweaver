import type { ReactNode } from 'react';
import type { ImageAsset } from '../types';

type DropColumnProps = {
  title: string;
  subtitle: string;
  ids: string[];
  imageById: Map<string, ImageAsset>;
  onDropImage: (id: string) => void;
  onCardClick: (id: string) => void;
  onReorder?: (from: number, to: number) => void;
  renderExtra?: (id: string) => ReactNode;
  renderActions?: (id: string) => ReactNode;
};

export function DropColumn({
  title,
  subtitle,
  ids,
  imageById,
  onDropImage,
  onCardClick,
  onReorder,
  renderExtra,
  renderActions
}: DropColumnProps) {
  return (
    <div
      className="rounded-lg border border-panelSoft bg-panelSoft/30 p-2"
      onDragOver={(e) => e.preventDefault()}
      onDrop={(e) => {
        e.preventDefault();
        const imageId = e.dataTransfer.getData('application/docweaver-image-id');
        if (imageId) onDropImage(imageId);
      }}
    >
      <div className="mb-2 px-1">
        <div className="text-sm font-medium">{title}</div>
        <div className="text-xs text-muted">{subtitle}</div>
      </div>
      <div className="min-h-28 space-y-2">
        {ids.length === 0 && <div className="rounded bg-panel/50 p-2 text-xs text-muted">Drop images here.</div>}
        {ids.map((id, idx) => {
          const image = imageById.get(id);
          if (!image) return null;
          return (
            <div
              key={id}
              draggable
              onDragStart={(e) => {
                e.dataTransfer.setData('application/docweaver-image-id', id);
                e.dataTransfer.setData('application/docweaver-index', String(idx));
              }}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                if (!onReorder) return;
                const from = Number(e.dataTransfer.getData('application/docweaver-index'));
                if (!Number.isNaN(from)) onReorder(from, idx);
              }}
              className="cursor-move rounded border border-panelSoft bg-panel p-2 text-xs"
            >
              <button className="mb-1 w-full text-left" onClick={() => onCardClick(id)}>
                <img src={`/api/images/${id}/preview`} alt={image.displayName} className="mb-1 h-20 w-full rounded object-cover" />
              </button>
              <div className="mb-1 flex items-center gap-2">
                <button className="min-w-0 flex-1 text-left" title={image.displayName} onClick={() => onCardClick(id)}>
                  <div className="truncate font-medium">{image.displayName}</div>
                </button>
                {renderActions && <div>{renderActions(id)}</div>}
              </div>
              {renderExtra?.(id)}
            </div>
          );
        })}
      </div>
    </div>
  );
}
