# ADR-010: JWT Authentication with Refresh Tokens

## Decision
Use JWT (JSON Web Token) for authentication with short-lived access tokens and long-lived refresh tokens. Session-based authentication is not used.

## Context
SmartFinances has a React frontend and Spring Boot backend running as separate services. Authentication must work across domains, scale horizontally on AWS, and support mobile clients in future.

## Approach

**Two token model:**

| Token | Lifetime | Stored | Purpose |
|-------|----------|--------|---------|
| Access Token | 15 minutes | Client memory | Authenticate every request |
| Refresh Token | 7 days | Database + client | Obtain new access token |

**Flow:**
```
Login → Access Token (15 min) + Refresh Token (7 days)
Every request → Authorization: Bearer {access_token}
Access token expires → POST /auth/refresh with refresh token → new access token
Logout → delete refresh token from database → user cannot renew
```

## Reasoning

| Session Problem | JWT Solution |
|----------------|-------------|
| Server stores session in memory — scales poorly | Stateless — server stores nothing per user |
| Multiple servers need shared session store | Any server verifies signature independently |
| Cookie-based — awkward cross-domain and mobile | Plain string in Authorization header — works everywhere |
| CSRF vulnerable — browser sends cookies automatically | Authorization header never sent automatically |
| Database lookup every request for validation | Cryptographic signature verification — no database needed |

## Revocation Strategy
Access tokens cannot be revoked early — acceptable given 15 minute lifetime. Refresh tokens are stored in `refresh_tokens` table and can be deleted immediately to force re-authentication.

## Impact
- `refresh_tokens` table required in database
- Spring Security configured for stateless sessions
- All endpoints except `/auth/**` require valid access token
- Frontend stores access token in memory, refresh token in httpOnly cookie
