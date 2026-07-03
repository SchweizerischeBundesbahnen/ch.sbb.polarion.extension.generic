/*
 * Build-time info for the generic UI bundle. This file is filtered at build time (see the
 * maven-resources-plugin config in the parent pom, which filters ONLY this file with the default
 * `${}` delimiters): Maven replaces `${maven.build.timestamp}`. It has no template literals, so
 * filtering is safe. Digits only, so it is directly numeric-comparable across generic builds
 * (a newer generic build yields a larger number). In unit tests the token stays unfiltered and
 * reduces to '' → 0, which is fine.
 */
// Normalize to the first 12 digits (yyyyMMddHHmm), so both the ISO default (yyyyMMddHHmmss) and a
// configured `yyyy-MM-dd HH:mm` reduce to the same minute-precision, comparable number regardless of
// which timestamp format the build happens to use.
export const GENERIC_BUILD_TIMESTAMP = '${maven.build.timestamp}'.replace(/\D/g, '').slice(0, 12);
