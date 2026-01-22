CREATE TABLE IF NOT EXISTS issues (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
  ticket_id VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  priority VARCHAR(32) NOT NULL,
  due_date TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_issue_ticket UNIQUE (workspace_id, ticket_id)
);

CREATE TABLE IF NOT EXISTS issue_labels (
  issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
  label VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_issues_workspace ON issues(workspace_id);
CREATE INDEX IF NOT EXISTS idx_issues_ticket ON issues(workspace_id, ticket_id);
CREATE INDEX IF NOT EXISTS idx_issues_assignee ON issues(workspace_id, assignee_id);
CREATE INDEX IF NOT EXISTS idx_issue_labels_issue ON issue_labels(issue_id);