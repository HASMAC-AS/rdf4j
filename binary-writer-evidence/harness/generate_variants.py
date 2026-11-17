#!/usr/bin/env python3
"""Generate isolated experiments from the immutable upstream writer, not production."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
BASE = (ROOT / "original/BinaryQueryResultWriter.java").read_text()


def replace(source, before, after):
    assert source.count(before) == 1, (before[:100], source.count(before))
    return source.replace(before, after)


def columns(source):
    source = source.replace("import org.eclipse.rdf4j.query.impl.ListBindingSet;\n", "")
    source = replace(source, "private BindingSet previousBindings;", "private Value[] previousValues;")
    source = replace(source,
        "\t\t\tList<Value> nullTuple = Collections.nCopies(this.bindingNames.size(), (Value) null);\n"
        "\t\t\tpreviousBindings = new ListBindingSet(this.bindingNames, nullTuple);",
        "\t\t\tpreviousValues = new Value[this.bindingNames.size()];")
    source = replace(source,
        "\t\t\t\tfor (String bindingName : bindingNames) {\n"
        "\t\t\t\t\tValue value = bindingSet.getValue(bindingName);",
        "\t\t\t\tfor (int column = 0; column < bindingNames.size(); column++) {\n"
        "\t\t\t\t\tValue value = bindingSet.getValue(bindingNames.get(column));\n"
        "\t\t\t\t\tValue previous = previousValues[column];")
    source = replace(source, "value.equals(previousBindings.getValue(bindingName))",
        "value == previous || previous != null && value.equals(previous)")
    source = replace(source,
        "\t\t\t\t\t\twriteValue(value);\n\t\t\t\t\t}\n\t\t\t\t}\n\n\t\t\t\tpreviousBindings = bindingSet;",
        "\t\t\t\t\t\twriteValue(value);\n\t\t\t\t\t}\n"
        "\t\t\t\t\tpreviousValues[column] = value;\n\t\t\t\t}")
    return source


OLD_STRING = '''\tprivate void writeString(String s) throws IOException {
\t\tByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
\t\tout.writeInt(byteBuf.remaining());
\t\tout.write(byteBuf.array(), 0, byteBuf.remaining());
\t}'''

ASCII_BODY = '''
\t\tint length = s.length();
\t\tif (length <= stringBuffer.length) {
\t\t\tint i = 0;
\t\t\twhile (i < length) {
\t\t\t\tchar c = s.charAt(i);
\t\t\t\tif (c > 0x7f) {
\t\t\t\t\tbreak;
\t\t\t\t}
\t\t\t\tstringBuffer[i++] = (byte) c;
\t\t\t}
\t\t\tif (i == length) {
\t\t\t\tout.writeInt(length);
\t\t\t\tout.write(stringBuffer, 0, length);
\t\t\t\treturn;
\t\t\t}
\t\t}
'''


def strings(source, mode):
    source = replace(source,
        "private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();",
        "private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();\n"
        "\tprivate final byte[] stringBuffer = new byte[4096];" +
        ("\n\tprivate final ByteBuffer encodedString = ByteBuffer.wrap(stringBuffer);" if mode == "reusable" else ""))
    body = ASCII_BODY
    if mode == "reusable":
        body += '''
\t\tif (length <= stringBuffer.length / 3) {
\t\t\tcharsetEncoder.reset();
\t\t\tencodedString.clear();
\t\t\tvar result = charsetEncoder.encode(CharBuffer.wrap(s), encodedString, true);
\t\t\tif (result.isError()) {
\t\t\t\tresult.throwException();
\t\t\t}
\t\t\tassert result.isUnderflow();
\t\t\tresult = charsetEncoder.flush(encodedString);
\t\t\tif (result.isError()) {
\t\t\t\tresult.throwException();
\t\t\t}
\t\t\tassert result.isUnderflow();
\t\t\tint bytes = encodedString.position();
\t\t\tout.writeInt(bytes);
\t\t\tout.write(stringBuffer, 0, bytes);
\t\t\treturn;
\t\t}
'''
    if mode == "utf8":
        source = source.replace("import java.nio.charset.CharsetEncoder;", "import java.nio.charset.CharsetEncoder;\nimport java.nio.charset.MalformedInputException;")
        body += '''
\t\tif (length <= stringBuffer.length / 3) {
\t\t\tint bytes = 0;
\t\t\tfor (int i = 0; i < length; i++) {
\t\t\t\tchar c = s.charAt(i);
\t\t\t\tif (c < 0x80) {
\t\t\t\t\tstringBuffer[bytes++] = (byte) c;
\t\t\t\t} else if (c < 0x800) {
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0xc0 | c >> 6);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | c & 0x3f);
\t\t\t\t} else if (!Character.isSurrogate(c)) {
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0xe0 | c >> 12);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | c >> 6 & 0x3f);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | c & 0x3f);
\t\t\t\t} else {
\t\t\t\t\tif (!Character.isHighSurrogate(c) || i + 1 == length || !Character.isLowSurrogate(s.charAt(i + 1))) {
\t\t\t\t\t\tthrow new MalformedInputException(1);
\t\t\t\t\t}
\t\t\t\t\tint codePoint = Character.toCodePoint(c, s.charAt(++i));
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0xf0 | codePoint >> 18);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | codePoint >> 12 & 0x3f);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | codePoint >> 6 & 0x3f);
\t\t\t\t\tstringBuffer[bytes++] = (byte) (0x80 | codePoint & 0x3f);
\t\t\t\t}
\t\t\t}
\t\t\tout.writeInt(bytes);
\t\t\tout.write(stringBuffer, 0, bytes);
\t\t\treturn;
\t\t}
'''
    body += '''\t\tByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
\t\tout.writeInt(byteBuf.remaining());
\t\tout.write(byteBuf.array(), 0, byteBuf.remaining());
'''
    return replace(source, OLD_STRING, "\tprivate void writeString(String s) throws IOException {" + body + "\t}")


def datatypes(source):
    source = source.replace("import java.util.HashMap;", "import java.util.HashMap;\nimport java.util.IdentityHashMap;")
    source = source.replace("import org.eclipse.rdf4j.model.Value;", "import org.eclipse.rdf4j.model.Value;\nimport org.eclipse.rdf4j.model.base.CoreDatatype;")
    source = replace(source, "private int nextNamespaceID;",
        "private int nextNamespaceID;\n\tprivate final Map<CoreDatatype, byte[]> datatypeRecords = new IdentityHashMap<>();")
    source = replace(source, "\t\t\tnextNamespaceID = 0;", "\t\t\tnextNamespaceID = 0;\n\t\t\tdatatypeRecords.clear();")
    start = source.index("\tprivate void writeLiteral(Literal literal)")
    end = source.index("\n\tprivate static String getLanguageTag", start)
    source = source[:start] + '''\tprivate void writeLiteral(Literal literal) throws IOException {
\t\tString label = literal.getLabel();
\t\tCoreDatatype coreDatatype = literal.getCoreDatatype();
\t\tif (coreDatatype == CoreDatatype.RDF.LANGSTRING || coreDatatype == CoreDatatype.RDF.DIRLANGSTRING) {
\t\t\tout.writeByte(LANG_LITERAL_RECORD_MARKER);
\t\t\twriteString(label);
\t\t\twriteString(getLanguageTag(literal));
\t\t\treturn;
\t\t}
\t\tIRI datatype = literal.getDatatype();
\t\tif ((coreDatatype instanceof CoreDatatype.XSD || coreDatatype instanceof CoreDatatype.RDF
\t\t\t\t|| coreDatatype instanceof CoreDatatype.GEO) && datatype == coreDatatype.getIri()) {
\t\t\tbyte[] record = datatypeRecords.get(coreDatatype);
\t\t\tif (record == null) {
\t\t\t\tInteger namespaceId = namespaceTable.get(datatype.getNamespace());
\t\t\t\tif (namespaceId == null) {
\t\t\t\t\tnamespaceId = writeNamespace(datatype.getNamespace());
\t\t\t\t}
\t\t\t\tbyte[] localName = datatype.getLocalName().getBytes(StandardCharsets.UTF_8);
\t\t\t\trecord = new byte[9 + localName.length];
\t\t\t\tByteBuffer.wrap(record).put((byte) QNAME_RECORD_MARKER).putInt(namespaceId).putInt(localName.length).put(localName);
\t\t\t\tdatatypeRecords.put(coreDatatype, record);
\t\t\t}
\t\t\tout.writeByte(DATATYPE_LITERAL_RECORD_MARKER);
\t\t\twriteString(label);
\t\t\tout.write(record);
\t\t} else {
\t\t\tInteger namespaceId = namespaceTable.get(datatype.getNamespace());
\t\t\tif (namespaceId == null) {
\t\t\t\tnamespaceId = writeNamespace(datatype.getNamespace());
\t\t\t}
\t\t\tout.writeByte(DATATYPE_LITERAL_RECORD_MARKER);
\t\t\twriteString(label);
\t\t\tout.writeByte(QNAME_RECORD_MARKER);
\t\t\tout.writeInt(namespaceId);
\t\t\twriteString(datatype.getLocalName());
\t\t}
\t}
''' + source[end:]
    return source


def buffered(source):
    source = source.replace("import java.io.DataOutputStream;", "import java.io.DataOutputStream;\nimport java.io.BufferedOutputStream;")
    return replace(source, "this.out = new DataOutputStream(out);", "this.out = new DataOutputStream(new BufferedOutputStream(out, 8192));")


def batch_columns(source):
    source = replace(source, "private Value[] previousValues;", "private Value[] previousValues;\n\tprivate byte[] markerBuffer;")
    source = replace(source, "previousValues = new Value[this.bindingNames.size()];", "previousValues = new Value[this.bindingNames.size()];\n\t\t\tmarkerBuffer = new byte[this.bindingNames.size()];")
    source = replace(source, "\t\t\t\tfor (int column = 0;", "\t\t\t\tint markerCount = 0;\n\t\t\t\tfor (int column = 0;")
    source = replace(source, "\t\t\t\t\t\twriteNull();", "\t\t\t\t\t\tmarkerBuffer[markerCount++] = NULL_RECORD_MARKER;")
    source = replace(source, "\t\t\t\t\t\twriteRepeat();", "\t\t\t\t\t\tmarkerBuffer[markerCount++] = REPEAT_RECORD_MARKER;")
    source = replace(source, "\t\t\t\t\t\twriteValue(value);", "\t\t\t\t\t\twriteMarkers(markerCount);\n\t\t\t\t\t\tmarkerCount = 0;\n\t\t\t\t\t\twriteValue(value);")
    source = replace(source, "\t\t\t\t\tpreviousValues[column] = value;\n\t\t\t\t}", "\t\t\t\t\tpreviousValues[column] = value;\n\t\t\t\t}\n\t\t\t\twriteMarkers(markerCount);")
    source = replace(source, "\tprivate void writeNull() throws IOException {", '''\tprivate void writeMarkers(int count) throws IOException {
\t\tif (count == 1) {
\t\t\tout.writeByte(markerBuffer[0]);
\t\t} else if (count > 1) {
\t\t\tout.write(markerBuffer, 0, count);
\t\t}
\t}

\tprivate void writeNull() throws IOException {''')
    return source


def frame_strings(source):
    source = replace(source, "new byte[4096]", "new byte[4100]")
    source = replace(source, "length <= stringBuffer.length)", "length <= stringBuffer.length - 4)")
    source = replace(source, "stringBuffer[i++] = (byte) c;", "stringBuffer[4 + i++] = (byte) c;")
    source = replace(source, "\t\t\t\tout.writeInt(length);\n\t\t\t\tout.write(stringBuffer, 0, length);", "\t\t\t\twriteBufferedString(length);")
    source = replace(source, "length <= stringBuffer.length / 3", "length <= (stringBuffer.length - 4) / 3")
    source = replace(source, "\t\t\tint bytes = 0;", "\t\t\tint bytes = 4;")
    source = replace(source, "\t\t\tout.writeInt(bytes);\n\t\t\tout.write(stringBuffer, 0, bytes);", "\t\t\twriteBufferedString(bytes - 4);")
    source = replace(source, "\tprivate void writeString(String s) throws IOException {", '''\tprivate void writeBufferedString(int length) throws IOException {
\t\tstringBuffer[0] = (byte) (length >>> 24);
\t\tstringBuffer[1] = (byte) (length >>> 16);
\t\tstringBuffer[2] = (byte) (length >>> 8);
\t\tstringBuffer[3] = (byte) length;
\t\tout.write(stringBuffer, 0, length + 4);
\t}

\tprivate void writeString(String s) throws IOException {''')
    return source


variants = {
    "Columns": columns(BASE),
    "Ascii": strings(BASE, "ascii"),
    "Reusable": strings(BASE, "reusable"),
    "Utf8": strings(BASE, "utf8"),
    "Datatype": datatypes(BASE),
    "Buffered": buffered(BASE),
    "Combined": buffered(datatypes(strings(columns(BASE), "utf8"))),
    "CombinedReusable": buffered(datatypes(strings(columns(BASE), "reusable"))),
    "Lean": batch_columns(datatypes(strings(columns(BASE), "utf8"))),
    "LeanReusable": batch_columns(datatypes(strings(columns(BASE), "reusable"))),
    "LeanAscii": batch_columns(datatypes(strings(columns(BASE), "ascii"))),
    "Framed": frame_strings(batch_columns(datatypes(strings(columns(BASE), "utf8")))),
}
for name, source in variants.items():
    target = ROOT / (name + "BinaryQueryResultWriter.java")
    target.write_text(source.replace("BinaryQueryResultWriter", name + "BinaryQueryResultWriter"))
    print(target.name)
