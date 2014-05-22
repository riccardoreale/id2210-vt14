package common.utils;

import java.util.List;
import java.util.Collection;
import java.util.LinkedList;

public class FuncTools {
	
	public interface Proposition<T> {
		public boolean eval (T param);
	}
	
	public static <T> List<T> filter (Collection<T> c, Proposition<T> p) {
		List<T> out = new LinkedList<T>();
		for (T i : c) {
			if (p.eval(i)) {
				out.add(i);
			}
		}
		return out;
	}

}
