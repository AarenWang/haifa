import { useState, useRef, useCallback } from 'react';
import {
  Upload,
  FileText,
  Trash2,
  Eye,
  X,
  CheckSquare,
  Square,
  Loader2,
  AlertCircle,
} from 'lucide-react';
import type { UploadRecord } from '../types';
import { uploadFile, deleteUpload, getUploadContent } from '../api/deerflowClient';

interface UploadPanelProps {
  uploads: UploadRecord[];
  selectedUploadIds: string[];
  threadId?: string;
  onUploadsChange: (uploads: UploadRecord[]) => void;
  onToggleSelection: (fileId: string) => void;
  onRemoveUpload: (fileId: string) => void;
}

export default function UploadPanel({
  uploads,
  selectedUploadIds,
  threadId,
  onUploadsChange,
  onToggleSelection,
  onRemoveUpload,
}: UploadPanelProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [previewFile, setPreviewFile] = useState<{
    fileId: string;
    fileName: string;
    content: string;
  } | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);
      const files = Array.from(e.dataTransfer.files);
      await uploadFiles(files);
    },
    [threadId]
  );

  const handleFileInputChange = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files ? Array.from(e.target.files) : [];
      await uploadFiles(files);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    [threadId]
  );

  const uploadFiles = async (files: File[]) => {
    if (files.length === 0) return;
    setUploading(true);
    setUploadError(null);

    const newUploads: UploadRecord[] = [];
    for (const file of files) {
      try {
        const response = await uploadFile(file, threadId);
        newUploads.push({
          fileId: response.fileId,
          fileName: response.fileName,
          fileSize: response.fileSize,
          mimeType: response.mimeType,
          threadId,
          status: response.status,
          createdAt: response.createdAt,
          updatedAt: response.createdAt,
        });
      } catch (err) {
        setUploadError(`Failed to upload ${file.name}: ${(err as Error).message}`);
      }
    }

    if (newUploads.length > 0) {
      onUploadsChange([...newUploads, ...uploads]);
    }
    setUploading(false);
  };

  const handleDelete = async (fileId: string) => {
    try {
      await deleteUpload(fileId, threadId);
      onRemoveUpload(fileId);
    } catch (err) {
      setUploadError(`Failed to delete: ${(err as Error).message}`);
    }
  };

  const handlePreview = async (fileId: string, fileName: string) => {
    setPreviewLoading(true);
    try {
      const data = await getUploadContent(fileId, threadId);
      setPreviewFile({ fileId, fileName, content: data.content });
    } catch (err) {
      setUploadError(`Failed to preview: ${(err as Error).message}`);
    } finally {
      setPreviewLoading(false);
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const selectedCount = selectedUploadIds.length;

  return (
    <div className="upload-panel">
      <div className="upload-panel-header">
        <div className="upload-panel-title">
          <Upload size={14} style={{ marginRight: 6 }} />
          Uploads
        </div>
        {selectedCount > 0 && (
          <span className="selected-files-badge">{selectedCount} selected</span>
        )}
      </div>

      <div
        className={`upload-dropzone ${isDragging ? 'dragging' : ''} ${uploading ? 'uploading' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          className="upload-input"
          onChange={handleFileInputChange}
        />
        {uploading ? (
          <>
            <Loader2 size={20} className="upload-spinner" />
            <span className="upload-dropzone-text">Uploading…</span>
          </>
        ) : (
          <>
            <Upload size={20} className="upload-dropzone-icon" />
            <span className="upload-dropzone-text">Drop files or click to upload</span>
          </>
        )}
      </div>

      {uploadError && (
        <div className="upload-error">
          <AlertCircle size={14} />
          <span>{uploadError}</span>
          <button className="upload-error-dismiss" onClick={() => setUploadError(null)}>
            <X size={12} />
          </button>
        </div>
      )}

      <div className="upload-list">
        {uploads.length === 0 ? (
          <div className="upload-empty">No files uploaded yet</div>
        ) : (
          uploads.map((upload) => {
            const isSelected = selectedUploadIds.includes(upload.fileId);
            return (
              <div key={upload.fileId} className={`upload-item ${isSelected ? 'selected' : ''}`}>
                <button
                  type="button"
                  className="upload-item-checkbox"
                  onClick={() => onToggleSelection(upload.fileId)}
                  title={isSelected ? 'Deselect' : 'Select'}
                >
                  {isSelected ? (
                    <CheckSquare size={16} className="checked" />
                  ) : (
                    <Square size={16} />
                  )}
                </button>
                <div className="upload-item-info">
                  <div className="upload-item-name">
                    <FileText size={14} />
                    {upload.fileName}
                  </div>
                  <div className="upload-item-meta">
                    {formatSize(upload.fileSize)} · {upload.status}
                  </div>
                </div>
                <div className="upload-item-actions">
                  <button
                    type="button"
                    className="upload-item-btn"
                    onClick={() => handlePreview(upload.fileId, upload.fileName)}
                    title="Preview"
                  >
                    <Eye size={14} />
                  </button>
                  <button
                    type="button"
                    className="upload-item-btn danger"
                    onClick={() => handleDelete(upload.fileId)}
                    title="Delete"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {previewFile && (
        <div className="upload-preview-overlay" onClick={() => setPreviewFile(null)}>
          <div className="upload-preview-panel" onClick={(e) => e.stopPropagation()}>
            <div className="upload-preview-header">
              <span className="upload-preview-title">{previewFile.fileName}</span>
              <button className="upload-preview-close" onClick={() => setPreviewFile(null)}>
                <X size={16} />
              </button>
            </div>
            <div className="upload-preview-body">
              {previewLoading ? (
                <div className="upload-preview-loading">
                  <Loader2 size={20} className="upload-spinner" />
                  Loading…
                </div>
              ) : (
                <pre className="upload-preview-content">{previewFile.content}</pre>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
