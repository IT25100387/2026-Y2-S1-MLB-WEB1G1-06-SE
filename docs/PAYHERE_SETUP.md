# PayHere payments and refunds

Only Cash, Card and QR can create new settlements. Card collections at the POS,
invoice Pay dialogs and the customer parts store use PayHere's hosted checkout.
PayHere handles real card numbers and CVVs; the application uses a station guest
profile for billing metadata. A real card payment settles an invoice only after
a verified, signed server notification.

When PayHere is unavailable locally, the default sandbox configuration offers a
clearly labeled demo card checkout with editable, validated test card fields. It finishes at the
receipt/printing step without collecting money, changing stock, or settling a
real invoice. Demo attempts use separate `DEMO_*` states and cannot be refunded
as real payments. Receipt previews and their printed copies are marked DEMO.
Valid PayHere settings take priority over the demo flow.

For client demonstrations, an unconfigured backend on a local browser host also
opens a browser-only demo checkout using the current quote or invoice snapshot.
It sends no payment request and requires no backend restart. Enter test card
`4111 1111 1111 1111`, a current or future expiry, a three-digit security code,
and a cardholder name. **Use test details** fills a valid sample. Other card
numbers are rejected. Card fields are neither persisted nor sent to the API.
Completion shows a demo statement with a **Print statement** action. This local
fallback is restricted to localhost/loopback hosts; set
`VITE_CARD_DEMO_ENABLED=false` to disable it in a frontend build.

Set these environment variables before starting the backend:

To get the credentials for PayHere's own checkout UI in a client demonstration:

1. [Create a PayHere sandbox account](https://sandbox.payhere.lk/account/signup/createaccount) and sign in. Sandbox transactions simulate payments without charging money. The sandbox account is separate from a live merchant account. See [PayHere's sandbox guide](https://support.payhere.lk/sandbox-and-testing).
2. Open **Integrations** to obtain the Merchant ID. Register the frontend domain through **Add Domain/App** and obtain its Merchant Secret after approval. This secret belongs to the registered domain. See [JavaScript SDK setup](https://support.payhere.lk/api-%26-mobile-sdk/javascript-sdk).
3. For refunds, open **Settings → API Keys**, create an API key, enable **Automated Charging API**, save it and open **View Credentials**. Use its App ID and App Secret for `PAYHERE_APP_ID` and `PAYHERE_APP_SECRET`. For sandbox API keys, PayHere recommends `*` as the allowed domain when testing domain access. See [Refund API setup](https://support.payhere.lk/api-%26-mobile-sdk/refund-api).
4. Add the values from the table below to your backend's **IDE run configuration → Environment variables**. Set `PAYHERE_SANDBOX=true`. Also configure the station guest contact profile and `FRONTEND_URL` for the registered frontend. Configure `PAYHERE_NOTIFY_URL` as the publicly reachable HTTPS backend address plus `/api/pos/card/notify`; PayHere cannot deliver its payment notification to localhost. Restart the backend after changing its environment. A plain `.env` file is not automatically loaded by this Spring Boot application.
5. In the real **PayHere sandbox popup**, test with Visa `4916217501611292`, a future expiry, a valid name and a three-digit CVV. The local FuelCore preview's `4111 1111 1111 1111` card is a different test card and will not produce a successful PayHere sandbox payment. [Official sandbox test cards](https://support.payhere.lk/sandbox-and-testing).

Configured merchant settings and a valid guest profile automatically select
PayHere's checkout instead of the local preview. Refund credentials are needed
only for gateway refunds; they do not enable the card checkout popup on their
own. Store secrets in the backend environment, outside source control.

| Variable | Purpose |
| --- | --- |
| `PAYHERE_MERCHANT_ID` | Checkout merchant ID |
| `FRONTEND_URL` | Public frontend URL for checkout return/cancel links |
| `PAYHERE_MERCHANT_SECRET` | Checkout signing secret; keep on the server |
| `PAYHERE_NOTIFY_URL` | Public HTTPS backend URL ending in `/api/pos/card/notify` |
| `PAYHERE_SANDBOX` | `true` for testing; `false` for a configured live merchant |
| `PAYHERE_DEMO_ENABLED` | Defaults to `true`; set `false` to require PayHere locally |
| `PAYHERE_GUEST_EMAIL` | Station-owned email for the shared guest profile |
| `PAYHERE_GUEST_PHONE` | Station phone with country code, e.g. `+94…` |
| `PAYHERE_GUEST_ADDRESS` | Station postal address, 5–200 characters |
| `PAYHERE_GUEST_CITY` | Station city |
| `PAYHERE_GUEST_COUNTRY` | Defaults to `Sri Lanka` |
| `PAYHERE_APP_ID` | Merchant API App ID for refunds |
| `PAYHERE_APP_SECRET` | Merchant API App Secret for refunds; keep on the server |

PayHere still requires billing metadata. The application sends the fixed name
`Guest Customer` and the configured station contact profile instead of asking
customers for these details. Use real station details accepted by your merchant
account. Missing or invalid settings disable card checkout before stock or
payment records change outside the local demo environment. See the [PayHere JavaScript SDK documentation](https://support.payhere.lk/api-%26-mobile-sdk/javascript-sdk).

Demo checkout requires both sandbox mode and a `FRONTEND_URL` hosted on
`localhost`, `127.0.0.1`, or `::1`. It is unavailable for public frontend hosts
and whenever `PAYHERE_SANDBOX=false`. This prevents a demo confirmation from
settling a live payment. Use `PAYHERE_DEMO_ENABLED=false` and configure the live
merchant settings for production.

Staff can refund a fully paid invoice once. They can edit the amount, but a
partial refund also consumes that one refund. Pending, partial, overdue,
provisional, void and previously refunded invoices cannot receive a staff refund.
The server locks the invoice and checks its refund history for both invoice and
payment settlement requests; retrying the identical request key returns the
existing result instead of issuing another refund.

The refund form offers Cash and Card. Cash records a manual return, including
for a payment originally collected by card or QR. Staff must actually return
the cash. An unresolved PayHere refund blocks this change of method until it is
reconciled. Real Card refunds require the original verified card payment and
are sent through the PayHere Refund API; QR is not a refund method.

Local Card refunds use the same validated demo card checkout as invoice payments
when the gateway configuration is in demo mode or unavailable. Completion opens
a printable **Demo refund statement**. This preview transfers no money and
leaves the financial ledger unchanged. Only one refund preview per invoice is
allowed in the signed-in browser session, including after refreshing the page.
For older local servers that return 403 or 404 from the card configuration
route, checkout first verifies a signed-in supported role before opening the
browser-only demo. It grants no server access and does not send a payment or
refund request to the denied route.

Refund credentials are separate from checkout credentials. Configure the API key
permissions and allowed domains in PayHere. Live Merchant API access also needs
the backend server IP allowed by PayHere. See the [PayHere Refund API](https://support.payhere.lk/api-%26-mobile-sdk/refund-api).

Every card refund requires an idempotency key. Gateway attempts are committed
independently of the invoice transaction. A confirmed attempt can resume ledger
recording with the same key without sending the refund again. A timeout or
uncertain response leaves a `REVIEW` attempt and blocks another refund of that
payment. Compare it with the PayHere dashboard before reconciling. Do not delete
attempt records to retry a payment. Historical card settlements lacking a
verified gateway reference must be reconciled before refunding.

For an invoice containing several settlements, retain the same refund request
key when recovering a partially processed gateway request. Confirmed portions
are reused. Unknown portions require provider verification. This application
does not automatically retry an uncertain money-moving API request.

Tests use mocked SDK, callbacks and Merchant API responses; they do not charge
cards or issue real refunds. Complete a sandbox checkout and refund with your
merchant settings and public notification URL before enabling live payments.
