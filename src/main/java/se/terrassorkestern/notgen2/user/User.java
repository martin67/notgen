package se.terrassorkestern.notgen2.user;

import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import lombok.Data;

@Data
@Entity
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String username;
  private String password;
  private String fullname;
  private Boolean enabled;

  @ManyToMany
  @JoinTable(
      name = "user_role", 
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"), 
      inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")) 
  private Collection<Role> roles;

}