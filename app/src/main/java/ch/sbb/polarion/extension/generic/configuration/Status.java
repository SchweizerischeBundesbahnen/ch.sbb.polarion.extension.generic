package ch.sbb.polarion.extension.generic.configuration;

public enum Status {
    OK,
    WARNING,
    ERROR;

    public String toHtml() {
        String color = switch (this) {
            case OK -> "green";
            case WARNING -> "orange";
            case ERROR -> "red";
        };
        return String.format("<span style='color: %s;'>%s</span>", color, this.name());
    }
}
