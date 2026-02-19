# Project Instructions

## Encoding
- All source files are UTF-8.
- Write Korean text directly as UTF-8 characters in strings, comments, and identifiers.
- Do not use Unicode escape sequences like `\uXXXX` for readable Korean text.

## Exception
- Unicode escapes are allowed only when technically required (for example: BOM handling like `\uFEFF`, or regex/codepoint ranges where escapes improve correctness).
