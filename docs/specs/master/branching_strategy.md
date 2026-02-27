# SmartFinances — Branching Strategy

## Branch Structure

```
main
  └── develop
        └── feature/xxx
```

| Branch | Purpose | Who merges |
|--------|---------|------------|
| `main` | Production ready code only. Deployed to AWS. | From develop via PR after testing |
| `develop` | Integration branch. All features land here first. | From feature branches via PR |
| `feature/xxx` | Individual feature work. Cut from develop. | Into develop via PR |

---

## Branch Naming Convention

```
feature/{spec-name}
```

Examples:
```
feature/V1-create-users-table
feature/V2-create-refresh-tokens-table
feature/user-controller-auth
feature/household-crud
```

Always lowercase, hyphens not underscores, descriptive enough to know what it does.

---

## Workflow

```
1. Cut feature branch from develop
   git checkout develop
   git pull
   git checkout -b feature/your-feature-name

2. Implement feature — commit regularly with meaningful messages
   git commit -m "feat: add users migration and entity"

3. Push branch
   git push --set-upstream origin feature/your-feature-name

4. Open PR into develop
   - Title matches feature spec name
   - Description references the spec file path
   - Review against acceptance criteria before merging

5. Merge into develop
   Squash merge — keeps history clean

6. Move spec from in-progress to completed
   docs/specs/features/in-progress/ → docs/specs/features/completed/

7. Update context.md
   Move feature from in-progress to built
```

---

## Commit Message Convention

```
feat:     new feature
fix:      bug fix
refactor: code change that is not a feature or fix
chore:    setup, config, documentation
test:     adding or updating tests
```

Examples:
```
feat: add users table migration and entity
feat: add refresh token repository
fix: correct foreign key constraint on refresh tokens
chore: update context.md with completed features
refactor: extract user mapping to UserMapper
```

---

## PR Rules

- Every feature branch must have a PR — no direct commits to develop or main
- PR description must reference the spec file
- All acceptance criteria in the spec must be checked before merging
- Squash merge only — one clean commit per feature on develop

---

## Release to Production

When develop is stable and tested:

```
git checkout main
git merge develop
git push origin main
```

GitHub Actions CI/CD pipeline deploys to AWS automatically on push to main.

---

## Feature Spec Branch Section

Every feature spec must include this section:

```markdown
## Branch
Cut from: develop
Branch name: feature/{spec-name}
Merge to: develop via PR
Commit message: feat: {short description}
```
