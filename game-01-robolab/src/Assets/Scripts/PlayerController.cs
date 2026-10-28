using UnityEngine;

namespace RoboLab
{
    /// <summary>
    /// Управление роботом: бег, прыжок и двойной прыжок (кор-механика игры).
    /// </summary>
    [RequireComponent(typeof(Rigidbody2D))]
    public class PlayerController : MonoBehaviour
    {
        public float moveSpeed = 6.5f;
        public float jumpVelocity = 13.5f;
        public float gravityScale = 4.2f;

        private Rigidbody2D body;
        private bool grounded;
        private bool doubleJumpAvailable;
        private bool facingRight = true;

        public bool Grounded { get { return grounded; } }
        public int JumpCount { get; private set; }

        private void Awake()
        {
            body = GetComponent<Rigidbody2D>();
            body.gravityScale = gravityScale;
            body.freezeRotation = true;
            body.collisionDetectionMode = CollisionDetectionMode2D.Continuous;
        }

        /// <summary>Направление движения по горизонтали (-1..1).</summary>
        public void Move(float axis)
        {
            body.linearVelocity = new Vector2(axis * moveSpeed, body.linearVelocity.y);
            if (axis > 0.01f && !facingRight) Flip();
            else if (axis < -0.01f && facingRight) Flip();
        }

        private void Flip()
        {
            facingRight = !facingRight;
            var s = transform.localScale;
            s.x = -s.x;
            transform.localScale = s;
        }

        /// <summary>Прыжок; повторный вызов в воздухе — двойной прыжок.</summary>
        public void Jump()
        {
            if (grounded)
            {
                body.linearVelocity = new Vector2(body.linearVelocity.x, jumpVelocity);
                grounded = false;
                doubleJumpAvailable = true;
                JumpCount = 1;
            }
            else if (doubleJumpAvailable)
            {
                body.linearVelocity = new Vector2(body.linearVelocity.x, jumpVelocity * 0.92f);
                doubleJumpAvailable = false;
                JumpCount = 2;
            }
        }

        private void OnCollisionEnter2D(Collision2D collision)
        {
            foreach (ContactPoint2D contact in collision.contacts)
            {
                if (contact.normal.y > 0.5f)
                {
                    grounded = true;
                    doubleJumpAvailable = true;
                    JumpCount = 0;
                }
            }
        }

        private void OnCollisionExit2D(Collision2D collision)
        {
            grounded = false;
        }
    }
}
