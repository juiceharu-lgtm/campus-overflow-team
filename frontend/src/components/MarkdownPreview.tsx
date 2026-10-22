import Markdown, { defaultUrlTransform } from 'react-markdown';

export function MarkdownPreview({ source }: { source: string }) {
  return (
    <section className="markdown-preview" aria-label="Markdown 预览">
      <h3>预览</h3>
      {source.trim() ? (
        <div className="markdown-body">
          <Markdown skipHtml urlTransform={defaultUrlTransform}>{source}</Markdown>
        </div>
      ) : (
        <p className="muted">输入内容后将在这里实时预览。</p>
      )}
    </section>
  );
}
