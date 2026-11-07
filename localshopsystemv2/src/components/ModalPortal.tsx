import React, { ReactNode } from 'react';
import { createPortal } from 'react-dom';

/**
 * Monta un modal directamente en <body> para que no quede recortado por contenedores con
 * overflow/transform. Envuelve en .theme-light + .modal-root para conservar los colores y la
 * tipografía del panel (que fuera de .admin no se heredan).
 */
const ModalPortal = ({ children }: { children: ReactNode }) =>
    createPortal(<div className="modal-root theme-light">{children}</div>, document.body);

export default ModalPortal;
