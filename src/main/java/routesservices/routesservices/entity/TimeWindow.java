package routesservices.routesservices.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeWindow {

    @Column(name = "time_window_start")
    private OffsetDateTime start;

    @Column(name = "time_window_end")
    private OffsetDateTime end;
}
