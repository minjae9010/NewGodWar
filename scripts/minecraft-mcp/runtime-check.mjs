// Reject unpatched runtimes before importing dependencies or opening transports.
const [major, minor, patch] = process.versions.node.split('.').map(Number);
const minimum = { 22: [23, 4], 24: [22, 1], 26: [12, 1] }[major];
if (!minimum || minor < minimum[0] || (minor === minimum[0] && patch < minimum[1])) {
  throw new Error(`Unpatched or unsupported Node.js ${process.versions.node}. Use scripts/minecraft-mcp/Node.ps1 (22.23.4), or patched 24.22.1+/26.12.1+.`);
}
