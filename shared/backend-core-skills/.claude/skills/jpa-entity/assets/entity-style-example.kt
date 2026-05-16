package app.project.sample.domain

@Entity
@Table(
    name = "samples",
    indexes = [
        Index(
            name = "idx_samples_owner_id",
            columnList = "owner_id, createdDate DESC",
        ),
    ],
)
class Sample private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sample_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE])
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: Owner,

    @Column(nullable = false)
    var title: String,

    var description: String?,
) : BaseEntity() {
    @OneToMany(mappedBy = "sample")
    @OrderBy("orderIndex ASC, createdDate ASC")
    val images: MutableList<SampleImage> = mutableListOf()

    companion object {
        fun create(
            owner: Owner,
            title: String,
            description: String?,
        ): Sample = Sample(
            owner = owner,
            title = title,
            description = description,
        )
    }

    fun update(
        title: String,
        description: String?,
    ) {
        this.title = title
        this.description = description
    }
}
