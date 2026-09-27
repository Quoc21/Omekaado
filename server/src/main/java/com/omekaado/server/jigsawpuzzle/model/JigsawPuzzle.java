package com.omekaado.server.jigsawpuzzle.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "jigsaw_puzzle")
@Getter 
@Setter 
@NoArgsConstructor 
public class JigsawPuzzle {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    @NotNull 
    private Integer userId;

    @Column(name = "image_path")
    @NotBlank 
    @Size(max = 500)
    private String imagePath;    

    @Column(name = "seed")
    @NotNull 
    private Integer seed;

    @Column(name = "width")
    @NotNull 
    @Positive 
    private Integer width;

    @Column(name = "height")
    @NotNull 
    @Positive 
    private Integer height;

    @Column(name = "piece_number")
    @NotNull 
    @Min(4) @Max(1000)
    private Integer pieceNumber;

    @Column(name = "row_number")
    @NotNull 
    @Positive 
    private Integer rowNumber;

    @Column(name = "column_number")
    @NotNull 
    @Positive 
    private Integer columnNumber;

    @Column(name = "created_at")
    @CreationTimestamp 
    private LocalDateTime createdAt;

    // avoid a query to db if somewhere bypass create logic service to save entity to db (for weirdo)
    @AssertTrue(message = "row_number * column_number must be >= piece_number")
    private boolean isPieceNumberMatchRowCol() {
        if (rowNumber == null || columnNumber == null || pieceNumber == null) {
            return true;   // just for avoid NullPointerException, @NotNull on each property will handle detail
        }
        return rowNumber * columnNumber >= pieceNumber;
    }
}