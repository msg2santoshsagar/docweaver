import type { ReactNode } from 'react';
import { CloseIcon, IconButton } from './IconButton';

type ModalProps = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: 'sm' | 'lg';
};

export function Modal({ isOpen, onClose, title, children, size = 'lg' }: ModalProps) {
  if (!isOpen) return null;

  const maxWidth = size === 'sm' ? 'max-w-md' : 'max-w-6xl';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4">
      <div className={`max-h-[92vh] w-full ${maxWidth} overflow-hidden rounded-2xl border border-panelSoft bg-bg p-4 shadow-card`}>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-lg font-semibold">{title}</h3>
          <IconButton title="Close dialog" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </div>
        {children}
      </div>
    </div>
  );
}
