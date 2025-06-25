# *****************************************************************************
#  Copyright (c) 2025 Eclipse RDF4J contributors.
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Distribution License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/org/documents/edl-v10.php.
#
#  SPDX-License-Identifier: BSD-3-Clause
# *****************************************************************************

#!/usr/bin/env python3
"""
A simple utility to calculate SHA-256 checksums for files.
"""

import argparse
import hashlib


def sha256_checksum(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    parser = argparse.ArgumentParser(description="Compute SHA-256 checksum")
    parser.add_argument("file", help="Path to the file")
    args = parser.parse_args()
    print(sha256_checksum(args.file))


if __name__ == "__main__":
    main()
