# Portfolio projects and experience: done

What gives the Portfolio templates real project and work-history data instead
of free-form JSON nobody could edit. Everything listed here is built; the
sections below are kept as the record of what was done and the traps found
along the way.

## Done (backend)

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
2. ~~`experience_entries`~~ - done. `V25__experience_entries.sql`, the
   `ExperienceEntry` entity, repository, request/response records,
   `ExperienceEntryService` and a controller at
   `/api/websites/{websiteId}/experience`, with the same five endpoints and the
   same tenant re-check on every write. `PublicWebsiteResponse.experience[]` is
   populated alongside `projects[]`. `ExperienceEntryServiceTest` covers the
   ownership check on update and delete, append-to-end order, and a reorder
   that ignores another site's ids; removing the check fails two of its seven.
   `ExperienceEntryRepositoryTest` persists a row for real, per the warning
   below - the column is `entry_year`, for the reason V18 renamed the projects
   one.

**Frontend** - all done.
3. ~~`PublicWebsiteResponse` type~~ - `projects` and `experience` both there.
4. ~~Clients~~ - `lib/api/projects.ts` and `lib/api/experience.ts`.
5. ~~Editors~~ - `/manage/{id}/projects` and `/manage/{id}/experience`: list,
   add, edit, delete, reorder, and an image upload on projects.
6. ~~Nav~~ - both come from the template's own content plan
   (`lib/website/template-content.ts`), so Experience appears only on the two
   layouts that render a timeline, under the name that template uses. Both the
   setup shell and the site console read that plan.
7. ~~Templates~~ - they read `data.projects` and `site.experience`, falling
   back to the old section JSON when a site has none, so anything saved before
   these editors existed renders exactly as it did.
8. ~~Samples~~ - the developer sample carries its history on the new field and
   the freelancer sample deliberately keeps its own in the old section payload,
   which keeps both paths under test in the browser suite.

Note while doing 6: `app/s/[slug]/[section]/page.tsx` had no editor registered
for `event`, so the console's own sidebar offered the link and the link
redirected straight back to the dashboard. Every key a content plan can name
needs an entry in that map, or the nav lies.

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
