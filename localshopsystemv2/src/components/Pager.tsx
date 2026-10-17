import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

type Props = {
  page: { number: number; totalPages: number; first: boolean; last: boolean } | null;
  onPrev: () => void;
  onNext: () => void;
};

const Pager = ({ page, onPrev, onNext }: Props) => {
  if (!page || page.totalPages <= 1) return null;
  return (
    <nav className="pager" aria-label="Paginación">
      <button className="btn btn-sm" onClick={onPrev} disabled={page.first} aria-label="Página anterior">
        <ChevronLeft size={16} /> Anterior
      </button>
      <span>
        Página {page.number + 1} de {page.totalPages}
      </span>
      <button className="btn btn-sm" onClick={onNext} disabled={page.last} aria-label="Página siguiente">
        Siguiente <ChevronRight size={16} />
      </button>
    </nav>
  );
};

export default Pager;
