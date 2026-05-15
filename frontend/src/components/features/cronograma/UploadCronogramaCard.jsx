import React from 'react';
import { Upload, Download, Loader2 } from 'lucide-react';

const UploadCronogramaCard = ({ 
  proyectoId, 
  uploadMutation, 
  handleUploadClick, 
  handleFileChange, 
  handleDownload, 
  fileInputRef 
}) => {
  return (
    <div className="cronograma-card">
      <h3 className="card-title">Subir Cronograma</h3>
      <div 
        className={`upload-zone ${uploadMutation.isPending ? 'uploading' : ''}`}
        onClick={handleUploadClick}
      >
        <input 
          type="file" 
          ref={fileInputRef} 
          onChange={handleFileChange} 
          accept=".pdf" 
          style={{ display: 'none' }}
        />
        {uploadMutation.isPending ? (
          <Loader2 className="upload-icon animate-spin" size={40} />
        ) : (
          <Upload className="upload-icon" size={40} />
        )}
        <p className="upload-text">Cargar cronograma del proyecto en PDF</p>
        <p className="file-name">cronograma_{proyectoId}.pdf</p>
      </div>
      <button className="download-btn" onClick={handleDownload}>
        <Download size={18} />
        Descargar cronograma_{proyectoId}.pdf
      </button>
    </div>
  );
};

export default UploadCronogramaCard;
