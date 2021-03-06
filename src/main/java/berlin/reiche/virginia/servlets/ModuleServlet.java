package berlin.reiche.virginia.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;

import berlin.reiche.virginia.MongoDB;
import berlin.reiche.virginia.model.Course;
import berlin.reiche.virginia.model.CourseModule;
import berlin.reiche.virginia.model.User;
import berlin.reiche.virginia.scheduler.CourseSchedule;

/**
 * The main servlet of the application which handles all incoming HTTP requests.
 * 
 * @author Konrad Reiche
 * 
 */
@SuppressWarnings("serial")
public class ModuleServlet extends HttpServlet {

    /**
     * File path to the web resources.
     */
    private static final String LIST_SITE = "ftl/modules/list.ftl";
    private static final String FORM_SITE = "ftl/modules/form.ftl";
    private static final String COURSES_SITE = "ftl/modules/course-list.ftl";
    private static final String RESPONSIBLITIES_SITE = "ftl/modules/responsibilities.ftl";

    /**
     * Further constants.
     */
    private static final String SELECTED_USER = "user";

    /**
     * Singleton instance.
     */
    private static final ModuleServlet INSTANCE = new ModuleServlet();

    public final static String root = "/modules";

    /**
     * The constructor is private in order to enforce the singleton pattern.
     */
    private ModuleServlet() {

    }

    /**
     * Parses the HTTP request and writes the response by using the template
     * engine.
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String path = request.getPathInfo();

        Map<String, Object> data = AppServlet.getDefaultData();
        Writer writer = response.getWriter();

        if (path == null) {
            data.put("modules", MongoDB.getAll(CourseModule.class));
            AppServlet.processTemplate(LIST_SITE, data, response.getWriter());
        } else if (path.equals("/")) {
            response.sendRedirect("/modules");
        } else if (path.matches("/" + AppServlet.ID_REGEX)) {
            ObjectId moduleId = new ObjectId(path.substring("/".length()));
            CourseModule module = MongoDB.get(CourseModule.class, moduleId);
            data.put("module", module);
            AppServlet
                    .processTemplate(COURSES_SITE, data, response.getWriter());
        } else if (path.equals("/new")) {
            data.put(AppServlet.REQUEST_HEADLINE_VAR, "New Course Module");
            data.put("module", CourseModule.NULL_MODULE);
            AppServlet.processTemplate(FORM_SITE, data, writer);
        } else if (path.matches("/delete/" + AppServlet.ID_REGEX)) {
            ObjectId id = new ObjectId(path.substring("/delete/".length()));
            CourseModule module = MongoDB.get(CourseModule.class, id);
            deleteCourseModule(request, response, module);
        } else if (path.matches("/edit/" + AppServlet.ID_REGEX)) {
            ObjectId id = new ObjectId(path.substring("/edit/".length()));
            CourseModule module = MongoDB.get(CourseModule.class, id);
            data.put("module", module);
            data.put(AppServlet.REQUEST_HEADLINE_VAR, "Edit Course Module");
            AppServlet.processTemplate(FORM_SITE, data, response.getWriter());
        } else if (path.equals("/responsibilities")) {
            List<CourseModule> modules = MongoDB.getAll(CourseModule.class);
            List<User> lecturers = MongoDB.createQuery(User.class)
                    .filter("lecturer =", true).asList();

            String selectedUser = request.getParameter(SELECTED_USER);
            User user = MongoDB.get(User.class, selectedUser);
            data.put("user", user);
            data.put("modules", modules);
            data.put("lecturers", lecturers);
            AppServlet.processTemplate(RESPONSIBLITIES_SITE, data, writer);
        } else {
            AppServlet.processTemplate(AppServlet.NOT_FOUND_SITE, data, writer);
        }
    }

    /**
     * When a course module is deleted all reference to the course module or
     * courses of the course module have to be cleaned up.
     * 
     * This includes removing courses from the user's list for responsible
     * courses and removing schedule entries with the courses.
     * 
     * @param request
     *            provides request information for HTTP servlets.
     * @param response
     *            provides HTTP-specific functionality in sending a response.
     * @param module
     *            the module to be deleted.
     * @throws IOException
     *             if an input or output exception occurs.
     */
    private void deleteCourseModule(HttpServletRequest request,
            HttpServletResponse response, CourseModule module)
            throws IOException {

        List<Course> courses = module.getCourses();
        List<User> users = MongoDB.getAll(User.class);
        for (User user : users) {
            if (user.getResponsibleCourses().removeAll(courses)) {
                MongoDB.store(user);
            }
        }

        CourseSchedule schedule = MongoDB.get(CourseSchedule.class);
        for (Course course : courses) {
            schedule.unsetCourse(course);
        }
        MongoDB.store(schedule);

        MongoDB.delete(module);
        response.sendRedirect("/modules");
    }

    /**
     * Parses all user HTML form requests and handles them.
     */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String path = request.getServletPath() + request.getPathInfo();

        if ("/modules/new".equals(path)) {
            handleModuleForm(request, response, null);
        } else if (path.matches("/modules/edit/" + AppServlet.ID_REGEX)) {
            ObjectId id = new ObjectId(
                    path.substring("/modules/edit/".length()));
            CourseModule module = MongoDB.get(CourseModule.class, id);
            handleModuleForm(request, response, module);
        } else if (path.equals("/modules/responsibilities")) {

            String[] ids = request.getParameterValues("responsibility");
            String selectedUser = request.getParameter(SELECTED_USER);
            User user = MongoDB.get(User.class, selectedUser);
            user.getResponsibleCourses().clear();

            if (ids != null) {
                for (String id : ids) {
                    Course course = MongoDB.get(Course.class, new ObjectId(id));
                    user.addCourse(course);
                }
            }
            MongoDB.store(user);
            response.sendRedirect(path + "?user=" + selectedUser);
        }
    }

    /**
     * Handles a course module creation and modification request.
     * 
     * @param request
     *            provides request information for HTTP servlets.
     * @param response
     *            provides HTTP-specific functionality in sending a response.
     * @param oldModule
     *            The course module object if it is present, if it is present
     *            this is an entity modification request, else it is an entity
     *            creation request.
     * @throws IOException
     *             if an input or output exception occurs.
     */
    private void handleModuleForm(HttpServletRequest request,
            HttpServletResponse response, CourseModule oldModule)
            throws IOException {

        Map<String, Object> data = AppServlet.getDefaultData();

        String name = request.getParameter("name");
        int credits = Integer.valueOf(request.getParameter("credits"));
        String assessment = request.getParameter("assessment");
        String description = request.getParameter("description");
        CourseModule newModule = new CourseModule(name, credits, assessment,
                description);
        data.put("module", newModule);

        // Course types
        String[] types = request.getParameterValues("type");

        // Course durations
        String[] durations = request.getParameterValues("duration");

        // Course counts
        String[] counts = request.getParameterValues("count");

        // Number of equipment requirements for each course
        String[] equipmentCounts = request
                .getParameterValues("equipment-count");

        // Equipment names
        String[] equipments = request.getParameterValues("equipment");

        // Different quantities for the equipment requirements for all courses
        String[] equipmentQuantities = request.getParameterValues("quantity");

        int k = 0;
        List<Course> courses = new ArrayList<>();

        // For each defined course
        for (int i = 0; i < types.length; i++) {

            int duration = Integer.valueOf(durations[i]);
            int count = Integer.valueOf(counts[i]);
            Course course = new Course(types[i], duration, count);

            int equipmentCount = Integer.valueOf(equipmentCounts[i]);
            for (int j = 0; j < equipmentCount; j++) {

                int quantity = 0;
                if (!equipmentQuantities[k].equals("")) {
                    quantity = Integer.valueOf(equipmentQuantities[k]);
                }

                String constraint = equipments[k];
                if (quantity > 0) {
                    course.getEquipment().put(constraint, quantity);
                } else {
                    course.getEquipment().remove(constraint);
                }

                k++;
            }
            courses.add(course);
        }

        if (oldModule == null) {
            oldModule = newModule;
        } else {
            oldModule.setName(name);
            oldModule.setCredits(credits);
            oldModule.setAssessment(assessment);
            oldModule.setDescription(description);
            oldModule.getCourses().clear();
        }

        MongoDB.store(oldModule);
        oldModule.getCourses().addAll(courses);
        for (Course course : courses) {
            course.setModule(oldModule);
            MongoDB.store(course);
        }
        MongoDB.store(oldModule);
        response.sendRedirect("/modules");
    }

    /**
     * @return an singleton instance of {@link ModuleServlet}.
     */
    public static ModuleServlet getInstance() {
        return INSTANCE;
    }

}
