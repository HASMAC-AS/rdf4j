import zipfile,struct,sys
class Reader:
 def __init__(self,b):self.b=b;self.p=0
 def n(self,n):s=self.b[self.p:self.p+n];self.p+=n;return s
 def u1(self):return self.n(1)[0]
 def u2(self):return int.from_bytes(self.n(2),'big')
 def u4(self):return int.from_bytes(self.n(4),'big')
def read(b):
 r=Reader(b);r.n(8);cnt=r.u2(); cp=[None]*cnt;i=1
 while i<cnt:
  t=r.u1()
  if t==1:cp[i]=r.n(r.u2()).decode('utf8','replace')
  elif t in (3,4):cp[i]=(t,r.n(4).hex())
  elif t in (5,6):cp[i]=(t,r.n(8).hex());i+=1
  elif t in (7,8,16,19,20):cp[i]=(t,r.u2())
  elif t in (9,10,11,12,17,18):cp[i]=(t,r.u2(),r.u2())
  elif t==15:cp[i]=(t,r.u1(),r.u2())
  else:raise ValueError(t)
  i+=1
 def s(i):
  v=cp[i]
  if isinstance(v,str):return v
  if v[0] in (7,8,16,19,20):return s(v[1])
  if v[0] in (9,10,11,12):return s(v[1])+'.'+s(v[2])
  return str(v)
 r.n(6);r.n(2*r.u2())
 def attrs():
  a=[]
  for _ in range(r.u2()):a.append((s(r.u2()),r.n(r.u4())))
  return a
 for _ in range(r.u2()):r.n(6);attrs()
 for _ in range(r.u2()):
  access,name,desc=r.u2(),s(r.u2()),s(r.u2());a=attrs()
  print('METHOD',name,desc)
  for k,v in a:
   if k!='Code':continue
   c=Reader(v);c.n(4);code=c.n(c.u4());p=0
   while p<len(code):
    off=p;op=code[p];p+=1
    n=0
    if op in [16,18,21,22,23,24,25,54,55,56,57,58,169,188]:n=1
    elif op in [17,19,20,132,*range(153,169),*range(178,185),187,189,192,193,198,199]:n=2
    elif op==197:n=3
    elif op in [185,186,200,201]:n=4
    elif op==196:n=5 if code[p]==132 else 3
    elif op in (170,171):
     p=(p+3)//4*4
     if op==170:lo=int.from_bytes(code[p+4:p+8],'big',signed=True);hi=int.from_bytes(code[p+8:p+12],'big',signed=True);n=12+4*(hi-lo+1)
     else:n=8+8*int.from_bytes(code[p+4:p+8],'big')
    if op in (*range(178,186),187,189,192,193): print(' ',off,hex(op),s(int.from_bytes(code[p:p+2],'big')))
    p+=n
with zipfile.ZipFile(sys.argv[1]) as z:
 for n in sys.argv[2:]:
  print('\nCLASS',n);read(z.read(n+'.class'))
