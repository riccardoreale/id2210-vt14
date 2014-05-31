package common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class FuncTools {
	
	public interface Proposition<T> {
		public boolean eval (T param);
	}
	
	public static <T> List<T> filter (Proposition<T> p, Collection<T> c) {
		List<T> out = new ArrayList<T>(c.size());
		for (T i : c) {
			if (p.eval(i)) {
				out.add(i);
			}
		}
		return out;
	}

	public interface Transformation<Ts, Td> {
		public Td eval (Ts param);
	}

	public static <Ts, Td> List<Td> map (Transformation<Ts, Td> f, Collection<Ts> c) {
		List<Td> out = new ArrayList<Td>(c.size());
		for (Ts i : c) {
			out.add(f.eval(i));
		}
		return out;
	}

}
