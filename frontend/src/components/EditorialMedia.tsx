import React, { useEffect, useState } from 'react';

interface EditorialMediaProps {
  src?: string;
  alt: string;
  label: string;
  className?: string;
  style?: React.CSSProperties;
}

export const EditorialMedia: React.FC<EditorialMediaProps> = ({
  src,
  alt,
  label,
  className = '',
  style
}) => {
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setFailed(false);
  }, [src]);

  const canRenderImage = Boolean(src) && !failed;
  const mediaClassName = ['editorial-media', className].filter(Boolean).join(' ');

  return (
    <div className={mediaClassName} style={style}>
      {canRenderImage ? (
        <img src={src} alt={alt} onError={() => setFailed(true)} loading="lazy" />
      ) : (
        <div className="atelier-media-fallback" aria-label={`${label} image unavailable`}>
          <span aria-hidden="true">{label}</span>
        </div>
      )}
    </div>
  );
};
