package berlin.reiche.scheduler.model;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import berlin.reiche.scheduler.MongoDB;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 * A course module is a self-contained, formally structured learning experience.
 * A course module has one or more courses which have to be undertaken in oder
 * to complete this module.
 * 
 * @author Konrad Reiche
 * 
 */
@Entity("course_module")
public class CourseModule {

	@Id
	ObjectId id;
	String name;
	int credits;
	String assessmentType;

	/**
	 * List of courses assigned to the module each mapped to the number how
	 * often the same course is offered.
	 */
	@Embedded
	Map<Course, Integer> courses;

	/**
	 * This constructor is used by Morphia via Java reflections.
	 */
	@SuppressWarnings("unused")
	private CourseModule() {

	}

	/**
	 * Creates a new course module by assigning the parameters directly, except
	 * the id which is generated by the database after saving the object.
	 * 
	 * @param name
	 *            the name.
	 * @param credits
	 *            the credit points.
	 * @param assessmentType
	 *            the assessment type.
	 */
	public CourseModule(String name, int credits, String assessmentType) {
		super();
		this.name = name;
		this.credits = credits;
		this.assessmentType = assessmentType;
	}

	/**
	 * Stores a new course module in the database.
	 * 
	 * @param name
	 *            the name.
	 * @param credits
	 *            the credit points.
	 * @param assessmentType
	 *            the assessment type.
	 * @return the generated id.
	 */
	public static ObjectId saveCourseModule(String name, int credits,
			String assessmentType) {
		CourseModule module = new CourseModule(name, credits, assessmentType);
		return (ObjectId) MongoDB.getDatastore().save(module).getId();
	}

	/**
	 * Retrieves all course modules from the database.
	 * 
	 * @return the list of retrieved course modules.
	 */
	public static List<CourseModule> getAllCourseModules() {
		return MongoDB.getDatastore().find(CourseModule.class).asList();
	}

	public ObjectId getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getCredits() {
		return credits;
	}

	public String getAssessmentType() {
		return assessmentType;
	}

	public Map<Course, Integer> getCourses() {
		return courses;
	}
	
}
