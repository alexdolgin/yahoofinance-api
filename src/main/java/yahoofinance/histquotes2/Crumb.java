package yahoofinance.histquotes2;

import java.util.Objects;

/**
 * @author Dmytro.Sheyko
 */
public class Crumb {
    private final String value;
    private final boolean fresh;

    public Crumb(String value, boolean fresh) {
        this.value = Objects.requireNonNull(value);
        this.fresh = fresh;
    }

    public String getValue()
    {
        return value;
    }

    public boolean isFresh()
    {
        return fresh;
    }
}
