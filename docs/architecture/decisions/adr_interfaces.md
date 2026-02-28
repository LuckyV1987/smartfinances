# ADR-011: No Service Interfaces Unless Justified

## Decision
Service classes are concrete classes — not backed by interfaces — unless there is a genuine reason for an interface.

## Context
Enterprise Java applications traditionally use a `ServiceImpl` pattern where every service has a corresponding interface. This made sense in large teams with multiple implementations or heavy use of AOP proxies. For SmartFinances as a solo AI-assisted project with one implementation per service this pattern adds boilerplate without value.

## Reasoning
- Single implementation per service — interface adds no abstraction benefit
- Spring dependency injection works equally well with concrete classes
- Mockito mocks concrete classes without interfaces
- Fewer files means less context to load into agent sessions
- Complexity should be added when needed not upfront

## When interfaces ARE used
- Repository layer — Spring Data JPA requires interfaces
- External integrations — Claude API, AWS services — interfaces allow swapping implementations
- Any service with genuine multiple implementations

## Impact
- Service classes are concrete — no `UserServiceImpl` pattern
- If a second implementation is ever needed an interface will be extracted at that point
- All existing and future service specs follow this convention
