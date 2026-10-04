import { useEffect, useRef } from 'react';

type Props = {
  busy: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
};

export function DeleteDraftConfirmation({ busy, error, onCancel, onConfirm }: Props) {
  const dialog = useRef<HTMLElement>(null);
  const cancel = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const previousFocus = document.activeElement;
    cancel.current?.focus();
    return () => {
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
    };
  }, []);

  useEffect(() => {
    if (busy) dialog.current?.focus();
  }, [busy]);

  return <section
    ref={dialog}
    className="send-confirmation"
    role="dialog"
    aria-modal="true"
    aria-labelledby="delete-draft-title"
    aria-describedby="delete-draft-description"
    aria-busy={busy}
    tabIndex={-1}
    onKeyDown={(event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        if (!busy) onCancel();
      }
      if (event.key === 'Tab') {
        const buttons = Array.from(dialog.current?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? []);
        if (!buttons.length) {
          event.preventDefault();
          dialog.current?.focus();
        } else if (document.activeElement === dialog.current) {
          event.preventDefault();
          buttons[event.shiftKey ? buttons.length - 1 : 0].focus();
        } else if (event.shiftKey && document.activeElement === buttons[0]) {
          event.preventDefault();
          buttons[buttons.length - 1].focus();
        } else if (!event.shiftKey && document.activeElement === buttons[buttons.length - 1]) {
          event.preventDefault();
          buttons[0].focus();
        }
      }
    }}
  ><div>
    <h2 id="delete-draft-title">¿Eliminar este borrador?</h2>
    <p id="delete-draft-description">Esta comunicación no ha sido enviada y se eliminará de forma permanente.</p>
    {error && <p role="alert">{error}</p>}
    <div className="communication-editor-actions">
      <button ref={cancel} className="btn btn-secondary" type="button" disabled={busy} onClick={onCancel}>Cancelar</button>
      <button className="btn btn-danger-secondary" type="button" disabled={busy} onClick={onConfirm}>{busy ? 'Eliminando…' : 'Eliminar borrador'}</button>
    </div>
  </div></section>;
}
