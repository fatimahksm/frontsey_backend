# Portfolio projects: progress and what is left

Handover for the feature that gives Portfolio templates real project data
instead of free-form JSON nobody can edit.

## Done (backend only)

- `V15__portfolio_projects.sql` - new table, `ON DELETE CASCADE` from the
  website. Every column but `name` is nullable.
- `PortfolioProject` entity, `PortfolioProjectRepository`.
- `PortfolioProjectRequest` / `PortfolioProjectResponse` (tags split on read,
  so no client parses the comma-separated column).
- `PortfolioProjectService` - list / create / update / delete / reorder, gated
  on `MANAGE_THEME_AND_CONTENT`. Every write re-checks the project belongs to
  the website in the path, so an id alone cannot reach another site's row.
- `PortfolioProjectController` at `/api/websites/{websiteId}/projects`
  (`GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PUT /reorder`).
- `PublicWebsiteResponse.projects[]` populated in `PublicWebsiteService`.

116 backend tests pass. Nothing existing changed behaviour: menu sites and
portfolios with no projects simply get an empty list.

## Left to do

**Backend**
1. ~~Tests for `PortfolioProjectService`~~ - done. `PortfolioProjectServiceTest`
   covers the ownership check on update and delete, reorder (including ids from
   another website, which are ignored), and append-to-end sort order.
   `PortfolioProjectRepositoryTest` persists a row for real.
2. `experience_entries` - same shape (year, role, company, detail, sortOrder),
   same CRUD. Not started.

**Frontend** (nothing done at all)
3. `PublicWebsiteResponse` type: add `projects: PublicProject[]`.
4. `lib/api/projects.ts` - client for the five endpoints.
5. Dashboard **Projects** page: list, add, edit, delete, reorder, image upload
   (reuse `ImageUploadField` and the gallery page's patterns).
6. Add it to `WebsiteShell` nav, gated to `templateType === "PORTFOLIO"`.
7. Point the four templates at `data.projects` instead of
   `extra.ABOUT.projectMeta` / `workMeta` / `caseMeta`, keeping the old read as
   a fallback so the samples keep working until they are migrated.
8. Move the four sample sites onto the new field.

## Warnings for whoever continues

- **The test suite cannot catch a broken migration.** Tests run with
  `ddl-auto=create-drop` and Flyway disabled, so they build the schema from the
  entities and never execute a migration. Run the app against a real Postgres
  before believing it works.
- **This table was missing from the test schema entirely, and the build stayed
  green.** The column was `year`, which H2 reserves as an identifier, so
  Hibernate's `CREATE TABLE` failed, the failure was logged and swallowed, and
  no test noticed because no test touched the table. V18 renames the column to
  `project_year`; the Java field and the JSON both still say `year`. The
  general lesson: a green suite proves nothing about a table nothing queries -
  when you add an entity, add a `@DataJpaTest` that persists one row.
- **Rebuild fully after pulling.** A stale `target/classes` keeps old SQL
  resources, which is exactly how the earlier `theme_config` failure happened -
  Flyway reported success at version 12 while the entity expected 13.
- `PublicWebsiteResponse` gained a field, so any test constructing it directly
  needs the extra argument (`PublicWebsiteServiceTest` was already updated).
