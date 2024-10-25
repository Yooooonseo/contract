package test.contract.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;
    private String name;
    private LocalDate birthdate; //투자자 생년월일 추가

    private String email;
    private String password;
    private String phoneNumber;
    private String address;

    @Enumerated(EnumType.STRING)
    private MemberRole memberRole;


    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private VentureListInfo ventureListInfo;

    @OneToMany(mappedBy = "investor")
    private List<Investment> investments; // 투자 내역


    // 기본 생성자
    public Member() {}

    // 모든 필드를 포함한 생성자
    public Member(String name, LocalDate birthdate) {
        this.name = name;
        this.birthdate = birthdate;
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    // Member와 VentureListInfo 연결 설정 메소드 추가
    public void setVentureListInfo(VentureListInfo ventureListInfo) {
        this.ventureListInfo = ventureListInfo;
        if (ventureListInfo != null) {
            ventureListInfo.setMember(this);  // 연결된 VentureListInfo에 Member 설정
        }
    }

}
