import React from 'react';

type Props = {
  eyebrow?: string;
  title: string;
  sub?: string;
  actions?: React.ReactNode;
};

const PageHeader = ({ eyebrow = 'Kiosco Botanico', title, sub, actions }: Props) => (
  <header className="page-head">
    <div>
      <span className="eyebrow">{eyebrow}</span>
      <h1 className="page-title">{title}</h1>
      {sub && <p className="page-sub">{sub}</p>}
    </div>
    {actions && <div className="page-actions">{actions}</div>}
  </header>
);

export default PageHeader;
