import resource,subprocess,sys,time
start=time.monotonic();r=subprocess.run(sys.argv[1:]); u=resource.getrusage(resource.RUSAGE_CHILDREN)
print('PROCESS elapsedSeconds=%.3f maxRSSKiB=%d userSeconds=%.3f systemSeconds=%.3f exit=%d'%(time.monotonic()-start,u.ru_maxrss,u.ru_utime,u.ru_stime,r.returncode),flush=True);sys.exit(r.returncode)
