# Test Data

## Rules

<!-- Production data. Dedicated test accounts A and B only. Writes off by default (E2E_ALLOW_DATA_WRITES=false). Every write test restores state. Never store passwords or tokens in this file. -->

## Test users (roles only, no credentials)

| User | Purpose | Email env var | Has child? | Notes |
|---|---|---|---|---|
| A | Main test user; all UI tests log in as A | `E2E_USER_A_EMAIL` | | |
| B | "Other user" for RLS isolation tests | `E2E_USER_B_EMAIL` | | |

## Schema notes (fill from Supabase Table Editor)

| Data | Table | Owner/link column | Notes |
|---|---|---|---|
| Profile | | | |
| Children | | | |
| Sandhya slots | | | |
| Settings (leaderboard opt-in) | | | |

## Observed RLS behaviour (probe by hand with A's JWT, record before writing tests)

| # | Request (as user A) | Target | Expected | Observed status | Observed body | Date |
|---|---|---|---|---|---|---|
| 1 | GET own profile | A | rows | | | |
| 2 | GET other user's profile | B | `[]` | | | |
| 3 | GET own children | A | rows | | | |
| 4 | GET other user's children | B | `[]` | | | |
| 5 | PATCH other user's row (Phase 1) | B | 0 rows | | | |
| 6 | INSERT row with B's user id (Phase 1) | B | 42501 | | | |

## What each test reads / writes

| Test | Reads | Writes | Needs `E2E_ALLOW_DATA_WRITES` | Restores by |
|---|---|---|---|---|
| T1-T7 | | | no | |
| T8 | | | yes | |
| T9 | | | yes | |
| T10 | | | no | |
| T11 | | | no | |
| T12 | | | yes | |
| T13 | | | no | |
| T14 | | | no | |
| T15 | | | yes | |

## Manual reset (if a run dies midway)

<!-- Step-by-step: how to put A and B back to their baseline state by hand in the app or Supabase dashboard -->

## Baseline state

<!-- What A and B should look like before any run: tracked anushtanams, slots unmarked today, leaderboard setting value, children -->
