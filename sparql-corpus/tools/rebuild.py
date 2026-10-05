#!/usr/bin/env python3
"""Single reproducible entry point for all implemented source cohorts and integrity checks."""
import argparse, subprocess, sys
from pathlib import Path


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--download',action='store_true')
    args=parser.parse_args()
    tools=Path(__file__).resolve().parent
    commands=[
        ('build_corpus.py',['--download']if args.download else[]),
        ('optional_ports.py',[]),
        ('lazy_ports.py',[]),
        ('native_capture.py',[]),
        ('qlever_fixture.py',[]),
        ('finalize_corpus.py',[]),
        ('verify_corpus.py',[]),
    ]
    for name,arguments in commands:
        subprocess.run([sys.executable,str(tools/name),*arguments],check=True)


if __name__=='__main__':main()
