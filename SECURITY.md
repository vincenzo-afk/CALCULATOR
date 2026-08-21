# Security Policy

## Supported Versions

We actively support and provide security updates for the following versions of MathX:

| Version | Supported |
| ------- | --------- |
| 1.0.x   | ✅ Yes    |
| < 1.0   | ❌ No     |

## Reporting a Vulnerability

We take the security of MathX seriously. If you discover a security vulnerability, please do not disclose it publicly. Instead, report it through one of the following channels:

1.  **GitHub Issues**: Open a private issue on the [GitHub repository](https://github.com/vincenzo-afk/CALCULATOR/issues) if the platform supports private vulnerability reporting.
2.  **Email**: Contact the maintainer directly at `itsmebk2007@gmail.com`.

### Our Process

- We will acknowledge receipt of your report within 48 hours.
- We will provide an estimated timeline for a fix.
- We will notify you once the vulnerability has been resolved.

### Security Principles

MathX is built with a **Security First** mindset. The core math engine uses a custom recursive-descent parser to avoid the risks associated with dynamic code execution (`eval()`). We strictly whitelist allowed tokens and functions to prevent any form of injection or arbitrary code execution.
