# First focused baseline attempt

`validation/baseline-focused-v1` completed Maven successfully with 93 passing cases, zero failures, zero errors and zero skips. The wrapper then rejected it because the prescribed selection requires 106 cases. The old common frozen test tree contains 36 materializer cases and 10 coordinate cases. The selected current sources contain 40 and 19 respectively, accounting for the 13-case difference.

This is a validation-input error, not evidence that the missing 13 cases passed or failed. The first command, raw XML, runner-at-launch, class trees, dependency manifest and results remain intact. The corrected validation explicitly compiles the eight selected current test source files into an isolated copy of the fresh module test-support tree. Its source and executed-class identities are retained. No benchmark input or production source changes are involved.
