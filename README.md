# DocWeaver

DocWeaver is a local-first document workflow app for image-to-document processing.

- Runs fully on your machine
- No cloud services
- No authentication
- No external API dependencies

It helps you upload images, route them into standalone or grouped outputs, preview before running, and process safely with predictable file naming.

## Current Feature Set

### Upload + Image Workspace
- Multi-image upload
- Large-image support (up to 25MB per file)
- Internal chunked upload handling for large batches
- Auto name suggestion (sanitized) after upload
- Image pool with quick remove and remove-all actions

### Image Routing
- Drag images between:
  - Pool (unassigned)
  - Standalone
  - Group draft
- Standalone items can independently toggle `Create PDF`
- Group draft supports drag-drop reordering

### Saved Groups
- Create and update grouped documents
- Open saved group PDF preview
- Edit group name from PDF preview
- Delete group directly from Saved Groups list
- Saved groups are auto-queued for processing

### Viewers
- Image viewer:
  - Rename
  - Zoom (buttons + mouse wheel)
  - Rotate left/right
  - Pan while left mouse is pressed
  - Reset view
  - Delete image
- PDF preview:
  - Page thumbnails + click navigation
  - Drag-drop page reorder (draft and saved groups)
  - Zoom/rotate/pan
  - Per-page rotation persistence for saved groups
  - Remove page from group
  - If last page is removed, confirmation dialog deletes entire group

### Processing
- `Process Queue` panel shows exactly what will be generated
- Process button states:
  - `Process`
  - `Processing...` (spinner)
  - `Processed`
  - `Retry Process`
- Processing history with `SUCCESS` / `FAILED`

### Safety Behavior
- Originals are never deleted by default
- Optional delete-originals runs only after successful processing
- Dry-run mode validates without writing/deleting
- Path validation and traversal protection
- Output filename hygiene + collision-safe naming (`file`, `file-1`, `file-2`, ...)

## Tech Stack

### Backend
- Java 21
- Spring Boot
- PostgreSQL
- Apache PDFBox
- Docker

### Frontend
- React
- Vite
- TypeScript
- Tailwind CSS

### Orchestration
- docker-compose

## Project Structure

- `/Users/sagar/Documents/DocWeaver/docweaver-backend`
- `/Users/sagar/Documents/DocWeaver/docweaver-ui`
- `/Users/sagar/Documents/DocWeaver/docker-compose.yml`
- `/Users/sagar/Documents/DocWeaver/start.sh`
- `/Users/sagar/Documents/DocWeaver/stop.sh`
- `/Users/sagar/Documents/DocWeaver/restart.sh`
- `/Users/sagar/Documents/DocWeaver/setup.sh` (compat wrapper to `start.sh`)
- `/Users/sagar/Documents/DocWeaver/README.md`

## Run Locally

Prerequisite: Docker Desktop (or Docker Engine + Compose plugin)

Start:
```bash
./start.sh
```

Stop:
```bash
./stop.sh
```

Restart:
```bash
./restart.sh
```

Compatibility start command:
```bash
./setup.sh
```

Endpoints after startup:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api

## Script Behavior

- `start.sh`
  - Creates required local data folders
  - Builds and starts services
  - If already running, exits cleanly without forced restart
- `stop.sh`
  - Safe no-fail stop (prints already stopped if not running)
- `restart.sh`
  - Safe stop, then start (works even if app was already stopped)

## Local Storage

- Uploads: `/Users/sagar/Documents/DocWeaver/data/uploads`
- Generated outputs: `/Users/sagar/Documents/DocWeaver/data/output`
- Postgres data: `/Users/sagar/Documents/DocWeaver/data/postgres`

## Settings

Persisted in PostgreSQL (`AppConfig`) and applied to future operations:
- Default output folder
- Default standalone output type (`IMAGE` or `PDF`)
- Default delete-originals behavior
- Dry-run mode

## API Summary

- `POST /api/images/upload`
- `GET /api/images`
- `GET /api/images/{imageId}/preview`
- `PATCH /api/images/{imageId}/rename`
- `PATCH /api/images/{imageId}/mode`
- `DELETE /api/images/{imageId}`
- `POST /api/groups`
- `PUT /api/groups/{groupId}`
- `PUT /api/groups/{groupId}/reorder`
- `PATCH /api/groups/{groupId}/images/{imageId}/rotation`
- `DELETE /api/groups/{groupId}`
- `GET /api/groups`
- `POST /api/process`
- `GET /api/history`
- `GET /api/config`
- `PUT /api/config`
- `POST /api/suggestions/name`

## Suggested Name Contract

`POST /api/suggestions/name` always accepts:
- `fileName`
- `mimeType`
- `fileBytes`
- `context.grouped` (boolean)
- `context.category` (string, accepted for forward compatibility)

Current implementation returns a sanitized suggestion with source `BASIC`.
