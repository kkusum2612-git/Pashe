**Care Circle and SOS presentation**

This branch provides two features: consent-based Care Circle membership and a self-SOS button with a durable ten-second cancellation window. The supporting incident screen shows the request status, optional pickup address and location. Only the owner and their current active guardians can view an open incident; the owner can mark it resolved.

**Start locally**

From `E:\470\Pashe`, with Docker Desktop running:

```powershell
# Backend, in one terminal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Start-Sos-Demo.ps1
```

```powershell
# Frontend, in another terminal
Set-Location E:\470\Pashe\frontend
npm.cmd ci
npm.cmd run dev
```

The demo launcher selects the `pashe_sos_demo` PostgreSQL schema. It does not delete or alter existing schemas. Use this launcher consistently for the presentation. Stop an older backend listening on port 8080 before starting it. This schema has its own accounts: users from another schema are not automatically copied.

The existing dev seeder creates `tester@pashe.local` when enabled in your local environment, using your configured `SEED_TEST_PASSWORD`. You can instead register accounts through the application. Verify new accounts using the messages captured at `http://localhost:8025`. No guardian membership is created automatically.

**Demonstration sequence**

Use separate browsers or browser profiles for A (care recipient) and B (guardian). Ordinary tabs share login cookies.

1. Sign in with two verified accounts at `http://localhost:5173/#/auth/login`. Open **Guardian SOS** from the shared sidebar/dashboard or `http://localhost:5173/#/guardian`.
2. In A, choose **Care Circle**, enter B's email, select **Guardian**, tick consent and select **Send invitation**. Check for **PENDING** in My circle.
3. In B, choose **Care Circle → Incoming → Refresh**. Select **Accept**. Refresh A's list: both should show **ACTIVE**. A recipient cannot accept their own outgoing invitation.
4. In A, press **SOS**, then **Cancel SOS** while several seconds remain. Wait at least 15 seconds. Expect “SOS canceled” and no new incident.
5. In A, optionally enter a synthetic pickup address and press SOS again. Let the countdown finish. Open the current incident: expect **OPEN**.
6. In B, return to the SOS dashboard. Within a few seconds, A's open SOS should appear in the incident list. Open it to view the address and available location. The UI polls the server; there is no need to resend SOS to refresh it.
7. In A, select **Share live location** and allow permission, or deny it to demonstrate that SOS still works. With a valid point, B sees the map, timestamp and accuracy. Stop sharing retains the last point; Remove saved location deletes it. Only foreground sharing is supported.
8. In A, select **Mark resolved** and enter a note. Expect **RESOLVED** in A's history; B loses access to that incident. Resolve the current incident before starting another independent demonstration.
9. To demonstrate persistence, start a fresh SOS, wait for the countdown to appear, close A's tab and wait 15 seconds. Reopen it. The server should have activated the same request. Starting again while an incident is already open reuses that incident instead of duplicating it.
10. To demonstrate revocation, open an SOS and let B view it. In A's Care Circle select **Remove** and confirm. After refreshing, B must lose access. Reinstatement requires a new invitation and acceptance.

Other Care Circle actions: the recipient can cancel a pending invitation; the invitee can decline it; either participant can revoke an active membership. Trusted-neighbor membership does not grant guardian access to private SOS details. Self-invitations and duplicate pending/active pairs are rejected. Invitation responses for unknown email addresses remain generic.

This is a course prototype. An open SOS in this demo is visible to authorized guardians within the application; it does not contact emergency services. Do not place test calls to 999.

**Persistence and API**

- Migration `V20261118000100` creates `care_circle_memberships` and its consent-event history.
- Migration `V20261118000500` creates `self_sos_requests`, `self_sos_incidents` and `self_sos_locations`.
- Recipient-scoped PostgreSQL transaction locks and partial unique indexes prevent multiple active incidents or pending countdowns per person.
- Deadline and server time are returned to the UI. Cancellation is allowed strictly before the deadline. A scheduled worker retries due requests; browser timers never create incidents.
- Locations must be in range with valid accuracy and a newer timestamp, no more than five minutes old or 30 seconds in the future. Stopped locations are marked last-known after five minutes and purged after 24 hours. Location sharing is optional.
- Existing JWT cookies, verified/enabled-user checks and CSRF protections apply. API responses are not cached by the PWA and offline SOS mutations are not queued.

Routes under `/api/v1/guardian`:

| Operation | Route |
| --- | --- |
| Own incoming/outgoing Care Circle | GET `/care-circle?page=0` |
| Invite | POST `/care-circle/invitations` with email, relationshipType and consent |
| Accept/decline/cancel/revoke | POST `/care-circle/memberships/{id}/{action}` |
| Start SOS | POST `/sos/requests` with UUID idempotencyKey and optional pickupAddress |
| Latest / specific own request | GET `/sos/requests`, `/sos/requests/{id}` |
| Cancel | POST `/sos/requests/{id}/cancel` |
| Authorized incident list/detail | GET `/incidents?page=0`, `/incidents/{id}` |
| Resolve as owner | POST `/incidents/{id}/resolve` with reason |
| Share/remove own location | POST / DELETE `/location` |

**Validation**

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Guardian.ps1`. By default it uses a fresh retained validation schema so repeated test runs do not affect application data or login rate limits. It runs frontend tests, production PWA build, lint and backend tests. When switching between code branches, clean Maven build outputs first to prevent obsolete compiled classes from loading.

Validated on 2026-11-19: 25 backend tests and 26 frontend tests passed, including a real PostgreSQL concurrent activation/cancellation test, along with the production PWA build and ESLint. Backend validation used a clean Maven build and fresh isolated schema.
