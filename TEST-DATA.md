# Test Data

## Rules

- Tests run against **production data**. Only the dedicated test accounts A and B are used. Never a personal account, never real users.
- Writes are **off by default** (`E2E_ALLOW_DATA_WRITES=false`), locally and in Jenkins. Write tests skip unless it is explicitly enabled for a run.
- Every write test **restores the original state** (toggle back, unmark slot). If a run dies midway, use the manual reset below.
- Never delete anushtanams through the API or dashboard.
- This file holds **roles and structure only**. No passwords, tokens, or keys.

## Test users (roles only, no credentials)

| User | Purpose | Email env var | Has child? | Notes |
|---|---|---|---|---|
| A | Main test user; all UI tests log in as A | `E2E_USER_A_EMAIL` | yes (4 children, confirmed 2026-10-11) | The e2e test account |
| B | "Other user" for RLS isolation tests | `E2E_USER_B_EMAIL` | yes (2 children, confirmed 2026-10-11) | One of Sreeni's own Google sign-in accounts; owner OK'd it for testing (2026-10-11) |

## Schema notes (fill from Supabase Table Editor)

| Data | Table | Owner/link column | Notes |
|---|---|---|---|
| Profile | `profiles` | `id` (= auth user id) | Settings page saves via `PATCH /rest/v1/profiles` |
| Children | `family_members` | `parent_id` -> `profiles.id` | |
| Tracked practices (anushtanams) | `user_practices` | `owner_id` (+ optional `family_member_id`) | Adding one sends `POST /rest/v1/user_practices`; trigger `sandhya_eligibility` runs on insert |
| Practice catalogue | `practices` | n/a | Sandhya rows have `is_sandhyavandhanam = true` |
| Sandhya slot marks | `practice_logs` | `owner_id`; `user_practice_id` -> `user_practices` | `slot` in morning / afternoon / evening; `log_date`. Written via RPC `submit_practice_log` (security definer), not direct insert |
| Leaderboard opt-in | `profiles.leaderboard_opt_in` | `id` | boolean, default false. Leaderboard itself is read via RPC `get_leaderboard` |

Source: Supabase MCP, schema + `pg_policies` + column grants, 2026-10-11.

## RLS policies (as defined)

| Table | SELECT | INSERT | UPDATE | DELETE |
|---|---|---|---|---|
| `profiles` | own (`id = auth.uid()`) | own | own (no column limits) | own |
| `family_members` | own (`parent_id = auth.uid()`) | own | own | own |
| `user_practices` | own (`owner_id = auth.uid()`) | own | own | own |
| `practice_logs` | own | **no policy -> denied** | **no policy -> denied** | own |

## Known security finding (2026-10-11)

- **Users can edit server-owned fields on their own rows.** `authenticated` has UPDATE on every column of `profiles` (incl. `punya`, `current_streak`, `best_streak`, `freeze_credits`, `ad_free_until`, `referred_by`), and there is no trigger guarding them. RLS only checks the row is yours, not which columns change. Same pattern on `family_members` (child `punya`/streaks) and `user_practices` (`current_streak`).
- **Impact:** a logged-in user can call the REST API directly to inflate punya/streaks (leaderboard cheating) or set `ad_free_until` far in the future.
- **Status:** found by schema review, not exploited. Test T15 will cover it and is expected to FAIL until the app is fixed (e.g. revoke column UPDATE grants and move these fields behind security-definer RPCs, or a BEFORE UPDATE trigger).

## Observed RLS behaviour (probe by hand with A's JWT, record before writing tests)

| # | Request (as user A) | Target | Expected | Observed status | Observed body | Date |
|---|---|---|---|---|---|---|
| 1 | GET `profiles?select=id` (no filter) | all | only A's row | SQL ok | 1 row | 2026-10-11 |
| 2 | GET `profiles?id=eq.<B>` | B | `[]` | SQL ok | 0 rows | 2026-10-11 |
| 3 | GET `family_members?select=*` | A | A's children only | SQL ok | 4 rows (matches app) | 2026-10-11 |
| 4 | GET `family_members?parent_id=eq.<B>` | B | `[]` | SQL ok | 0 rows | 2026-10-11 |
| 5 | PATCH `profiles?id=eq.<B>` | B | 0 rows, B unchanged | SQL ok | 0 rows updated | 2026-10-11 |
| 6 | INSERT `family_members` with `parent_id=<B>` | B | 42501 | 42501 | `new row violates row-level security policy for table "family_members"` | 2026-10-11 |
| 7 | INSERT `practice_logs` directly, own row | A | 42501 (no insert policy) | 42501 | `new row violates row-level security policy for table "practice_logs"` | 2026-10-11 |
| T15 | PATCH own `profiles.punya` | A | rejected | **allowed (1 row updated) - bug confirmed** | n/a | 2026-10-11 |

Expected column = derived from the policies above. Observed 2026-10-11 by SQL Editor as A (`set local role authenticated` + `request.jwt.claims`, inside `begin ... rollback`, so no data changed). This proves the database rules; HTTP status codes are recorded when T11/T12 first run through the API in Phase 1.

## What each test reads / writes

| Test | Reads | Writes | Needs `E2E_ALLOW_DATA_WRITES` | Restores by |
|---|---|---|---|---|
| T1-T4 | Public login page, auth API | nothing | no | n/a |
| T5-T7 | A's dashboard, panchangam | nothing | no | n/a |
| T8 | A's sandhya slots | marks one slot for today | yes | unmark the same slot |
| T9 | A's profile | leaderboard opt-in toggle | yes | toggle back to original value |
| T10 | A's dashboard (one request blocked by BiDi) | nothing | no | n/a |
| T11 | A's rows, attempted read of B's rows | nothing | no | n/a |
| T12 | B's row (via service role, to confirm unchanged) | attempted update/insert on B's data (should be blocked) | yes | if RLS fails: restore B from baseline via service role |
| T13 | A's children, attempted read of B's children | nothing | no | n/a |
| T14 | A's session | nothing (logout) | no | n/a |
| T15 | A's profile (via service role, before/after) | attempted PATCH of A's own `punya` / `ad_free_until` (should be rejected) | yes | restore original values via service role (needed while the bug exists) |

## Manual reset (if a run dies midway)

<!-- Step-by-step: how to put A and B back to their baseline state by hand in the app or Supabase dashboard -->

## Baseline state

Recorded 2026-10-11. Every run must leave A and B exactly like this.

| User | Children | Tracked anushtanams | Leaderboard opt-in | Today's sandhya slots |
|---|---|---|---|---|
| A | 4 | 4 | ON | unmarked before a run |
| B | 2 | 5 | ON | not touched by tests |
