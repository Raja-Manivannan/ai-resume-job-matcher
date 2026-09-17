import { useState, useEffect, useRef } from "react";
import ReactMarkdown from "react-markdown";
import html2pdf from "html2pdf.js";
import "./App.css";

function App() {
  const [resumeText, setResumeText] = useState("");
  const [jobDescription, setJobDescription] = useState("");
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploadingResume, setUploadingResume] = useState(false);
  const [uploadingJob, setUploadingJob] = useState(false);
  const [error, setError] = useState("");
  const reportRef = useRef(null);

  const fetchHistory = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/resumes/history");
      if (response.ok) {
        const data = await response.json();
        setHistory(data.reverse());
      }
    } catch {
      // Fails silently if backend is offline
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handlePdfUpload = async (event, targetField) => {
    const file = event.target.files[0];
    if (!file) return;

    if (file.type !== "application/pdf") {
      setError("Please upload a valid .pdf file.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    if (targetField === "resume") setUploadingResume(true);
    if (targetField === "job") setUploadingJob(true);
    setError("");

    try {
      const response = await fetch("http://localhost:8080/api/resumes/upload-pdf", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const errData = await response.json();
        throw new Error(errData.error || "Failed to extract text from PDF.");
      }

      const data = await response.json();
      if (targetField === "resume") {
        setResumeText(data.text);
      } else {
        setJobDescription(data.text);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      if (targetField === "resume") setUploadingResume(false);
      if (targetField === "job") setUploadingJob(false);
    }
  };

  const handleAnalyze = async () => {
    if (!resumeText.trim() || !jobDescription.trim()) {
      setError("Please ensure both resume content and target job description are filled.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const response = await fetch("http://localhost:8080/api/resumes/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resumeText, jobDescription }),
      });

      if (!response.ok) {
        throw new Error("Analysis failed. Ensure your backend is reachable.");
      }

      const data = await response.json();
      setResult(data);
      fetchHistory();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const downloadPdfReport = () => {
    if (!reportRef.current) return;
    const opt = {
      margin: 15,
      filename: `ATS_Match_Report_${result.matchScore}pct.pdf`,
      image: { type: "jpeg", quality: 0.98 },
      html2canvas: { scale: 2 },
      jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    };
    html2pdf().set(opt).from(reportRef.current).save();
  };

  const loadPastScan = (item) => {
    setResumeText(item.resumeText);
    setJobDescription(item.jobDescription);
    setResult({
      matchScore: item.matchScore,
      feedback: item.feedback,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDelete = async (e, id) => {
    e.stopPropagation();
    try {
      const response = await fetch(`http://localhost:8080/api/resumes/history/${id}`, {
        method: "DELETE",
      });
      if (response.ok) {
        setHistory((prev) => prev.filter((item) => item.id !== id));
      } else {
        setError("Failed to delete history item.");
      }
    } catch {
      setError("Network error while deleting item.");
    }
  };

  return (
    <div className="container">
      <header className="header">
        <h1>AI Resume & Job Matcher</h1>
        <p>Upload your PDF files or paste plain text to evaluate role alignment.</p>
      </header>

      <div className="grid">
        <div className="input-group">
          <div className="label-row">
            <label>Your Resume Content</label>
            <label className="upload-btn">
              {uploadingResume ? "Extracting..." : "Upload PDF"}
              <input
                type="file"
                accept="application/pdf"
                onChange={(e) => handlePdfUpload(e, "resume")}
                disabled={uploadingResume}
                style={{ display: "none" }}
              />
            </label>
          </div>
          <textarea
            rows="10"
            placeholder="Paste resume text or click 'Upload PDF' above..."
            value={resumeText}
            onChange={(e) => setResumeText(e.target.value)}
          />
        </div>

        <div className="input-group">
          <div className="label-row">
            <label>Target Job Description</label>
            <label className="upload-btn">
              {uploadingJob ? "Extracting..." : "Upload PDF"}
              <input
                type="file"
                accept="application/pdf"
                onChange={(e) => handlePdfUpload(e, "job")}
                disabled={uploadingJob}
                style={{ display: "none" }}
              />
            </label>
          </div>
          <textarea
            rows="10"
            placeholder="Paste target job requirements or click 'Upload PDF' above..."
            value={jobDescription}
            onChange={(e) => setJobDescription(e.target.value)}
          />
        </div>
      </div>

      {error && <div className="error-badge">{error}</div>}

      <div className="action-row">
        <button
          className="btn-primary"
          onClick={handleAnalyze}
          disabled={loading || uploadingResume || uploadingJob}
        >
          {loading ? "Evaluating Match..." : "Analyze Match"}
        </button>
      </div>

      {result && (
        <div className="result-wrapper">
          <div className="result-actions">
            <button className="btn-export" onClick={downloadPdfReport}>
              Download PDF Report
            </button>
          </div>

          <div className="result-card" ref={reportRef}>
            <div className="score-header">
              <div>
                <h2 className="result-title">Match Assessment Report</h2>
                <span className="timestamp">{new Date().toLocaleDateString()}</span>
              </div>
              <div className="score-pill">{result.matchScore}% Match</div>
            </div>
            <div className="markdown-body">
              <ReactMarkdown>{result.feedback}</ReactMarkdown>
            </div>
          </div>
        </div>
      )}

      {history.length > 0 && (
        <div className="history-section">
          <h2>Previous Match History</h2>
          <div className="history-list">
            {history.map((scan) => (
              <div
                key={scan.id}
                className="history-item"
                onClick={() => loadPastScan(scan)}
              >
                <div className="history-info">
                  <span className="history-score">{scan.matchScore}%</span>
                  <span className="history-snippet">
                    {scan.resumeText.slice(0, 75)}...
                  </span>
                </div>
                <div className="history-actions">
                  <button className="history-view-btn">Review</button>
                  <button
                    className="history-delete-btn"
                    onClick={(e) => handleDelete(e, scan.id)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default App;