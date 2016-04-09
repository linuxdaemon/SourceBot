CREATE TABLE blogs (
  id                SERIAL  NOT NULL PRIMARY KEY,
  url               TEXT    NOT NULL,
  blog_check_active BOOLEAN NOT NULL DEFAULT TRUE,
  sample_size       INT     NOT NULL DEFAULT 1000,
  post_type         TEXT [] NOT NULL,
  post_select       TEXT    NOT NULL,
  post_state        TEXT    NOT NULL,
  post_buffer       INT     NOT NULL DEFAULT 20,
  post_comment      TEXT,
  post_tags         TEXT [],
  preserve_tags     BOOLEAN NOT NULL DEFAULT FALSE,
  active            BOOLEAN NOT NULL DEFAULT TRUE
);

