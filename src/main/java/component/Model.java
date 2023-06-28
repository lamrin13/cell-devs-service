package component;

import java.util.Arrays;

public class Model {

	private Object[] atomic;
	private Object[] coupled;
	private Object top;
	
	public Model(Object[] atomic, Object[] coupled, Object top) {
		super();
		this.atomic = atomic;
		this.coupled = coupled;
		this.top = top;
	}

	public Object[] getAtomic() {
		return atomic;
	}

	public void setAtomic(Object[] atomic) {
		this.atomic = atomic;
	}

	public Object[] getCoupled() {
		return coupled;
	}

	public void setCoupled(Object[] coupled) {
		this.coupled = coupled;
	}

	
	public Object getTop() {
		return top;
	}

	public void setTop(Object top) {
		this.top = top;
	}

	@Override
	public String toString() {
		return "Model [atomic=" + Arrays.toString(atomic) + ", coupled=" + Arrays.toString(coupled) + "]";
	}

	
}
