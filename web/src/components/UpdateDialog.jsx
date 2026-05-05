export function UpdateDialog({ version, url, changelog, onClose }) {
  const handleDownload = () => {
    window.open(url, '_blank')
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>发现新版本 v{version}</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        {changelog && (
          <div className="update-changelog">
            <p className="changelog-title">更新内容：</p>
            <p>{changelog}</p>
          </div>
        )}

        <div className="modal-actions">
          <button className="btn-secondary" onClick={onClose}>稍后再说</button>
          <button className="btn-primary" onClick={handleDownload}>
            立即更新
          </button>
        </div>
      </div>
    </div>
  )
}
