RT Rooms Hardening (feature disabled)

NOTE: The Real-time Rooms feature is disabled in this build. The following notes are retained for reference only.

Overview
- Added `updated_at` columns to `rt_invites` and `rt_members` (Flyway migrations V11 and V12).
- Added uniqueness constraint on `rt_invites(room_id, invited_user_id)` (V13) and on `rt_members(room_id, user_id)` (V12).
- Introduced idempotent invite semantics and reinvite behavior in `RtRoomService`.
- Enforced `max_members` capacity in `joinRoom` and added defensive handling for concurrent joins.
- Improved WebSocket handshake to reject missing/invalid tokens with HTTP 401 status.

Migration guidance
1. Review migrations in `src/main/resources/db/migration/` (V11, V12, V13). They:
   - create `updated_at` columns and backfill values from `created_at` / `joined_at`.
   - deduplicate existing rows (keep the earliest id) before adding unique constraints.

2. On large production datasets, the `DELETE ... WHERE id NOT IN (SELECT MIN(id) ...)` statements can be expensive. Consider:
   - Running `SELECT room_id, invited_user_id, COUNT(*) FROM rt_invites GROUP BY 1,2 HAVING COUNT(*) > 1` to inspect duplicates.
   - Performing deduplication in batches, or off-peak windows, and manually validating results before applying the unique constraint.

3. Test plan
   - After migration, verify that `rt_invites` rows have non-null `updated_at` values and that adding a duplicate invite fails due to unique constraint.
   - Verify that `rt_members` has `updated_at` populated and no duplicate `(room_id,user_id)` pairs.

Rollback
- If migration fails, restore from backup and run manual dedupe scripts before re-applying migrations.

Development notes
- Unit and integration tests added/updated:
  - `RtRoomInviteIntegrationTest` (idempotent accept/reject/reinvite)
  - `RtRoomMemberControlIntegrationTest` (maxMembers enforcement, join idempotency)
- Service-level changes are backward compatible and keep API contract stable.

If you want, I can:
- Add more detailed dedupe SQL examples for large tables.
- Create a small migration-run checklist for DBAs.
