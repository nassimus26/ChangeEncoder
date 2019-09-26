package tools;

public enum PatchOperation {
    patch, added, deleted, replace;

    @Override
    public String toString() {
        return "."+name();
    }
}
