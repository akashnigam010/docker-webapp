package in.akash.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import in.akash.db.entity.StudentEntity;

@Stateless(name = "env/in.akash.dao.StudentDao")
public class StudentDaoImpl implements StudentDao {
	private static final String STUDENT_LIST = "studentList";
	private final EmbeddedCacheManager cacheManager;
	private Cache<String, List<StudentEntity>> cache;
	private List<StudentEntity> studentList;

	@PersistenceContext(unitName = "primary")
	private EntityManager em;

	public StudentDaoImpl() {
		GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
	      global.transport().clusterName("StudentAppCluster");
	      cacheManager = new DefaultCacheManager(global.build());
	      ConfigurationBuilder config = new ConfigurationBuilder();
	      config
	          //.expiration().lifespan(10, TimeUnit.SECONDS)
	          .clustering().cacheMode(CacheMode.DIST_SYNC);
		
		//cacheManager = new DefaultCacheManager();
		//cacheManager.defineConfiguration("student", new ConfigurationBuilder().build());
	    cacheManager.defineConfiguration("student", config.build());
		cache = cacheManager.getCache("student");
	}

	public void saveStudent(StudentEntity student) {
		em.persist(student);
		System.out.println("New Student Saved : " + student.toString());
		//reset cache
		cache.clear();
	}

	public List<StudentEntity> getAllStudents() {
		studentList = (List<StudentEntity>) cache.get(STUDENT_LIST);
		if (studentList == null) {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
			Root<StudentEntity> from = cq.from(StudentEntity.class);
			CriteriaQuery<StudentEntity> all = cq.select(from);
			TypedQuery<StudentEntity> allQuery = em.createQuery(all);
			studentList = allQuery.getResultList();
			cache.put(STUDENT_LIST, studentList);
		}
		return studentList;
	}
}
